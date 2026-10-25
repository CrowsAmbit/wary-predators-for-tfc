package com.crowsambit.warypredators;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import net.dries007.tfc.common.player.IPlayerInfo;
import net.dries007.tfc.common.entities.predator.Predator;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;
@GameTestHolder("warypredators")
@PrefixGameTestTemplate(false)
public class WaryGameTests {
    static ServerPlayer player(GameTestHelper h) {
        for(int x=0;x<10;x++) for(int z=0;z<10;z++) {
            h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
            for(int y=1;y<=4;y++) h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
        }
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"companion-test");
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),connection,p,cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener) {}
        };
        h.getLevel().addNewPlayer(p);
        p.teleportTo(h.absoluteVec(new Vec3(4,1,4)).x,h.absoluteVec(new Vec3(4,1,4)).y,h.absoluteVec(new Vec3(4,1,4)).z);
        p.setHealth(p.getMaxHealth()); p.getFoodData().setFoodLevel(20); IPlayerInfo.get(p).setThirst(100); return p;
    }

 static Predator predator(GameTestHelper h) {
  var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("tfc:polar_bear"));
  var animal=(Predator)h.spawn(type,5,1,4);animal.setNoAi(true);return animal;
 }
 static Optional<? extends LivingEntity> gate(Predator a, ServerPlayer p) {
  var ci=new CallbackInfoReturnable<Optional<? extends LivingEntity>>("test",true,Optional.of(p));
  WaryPredatorsHooks.gateAttackTarget(a,ci);return ci.getReturnValue();
 }
 static void target(Predator a, LivingEntity p) {a.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,p);}
 @GameTest(template="arena")
 public static void projectileTargetsShooter(GameTestHelper h) {
  var p=player(h);var a=predator(h);p.setPos(a.getX()+30,a.getY(),a.getZ());
  var arrow=new Arrow(EntityType.ARROW,h.getLevel());arrow.setOwner(p);
  h.assertTrue(a.hurt(h.getLevel().damageSources().arrow(arrow,p),1),"Projectile damage must succeed");
  h.assertTrue(a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==p,"Actual hurt mixin must resolve shooter outside provoke distance");
  WaryPredatorsHooks.tickBreakOff(a);
  h.assertTrue(a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==p,"Withdrawal cannot clear fresh retaliation");
  p.discard();h.succeed();
 }
 @GameTest(template="arena")
 public static void warningIsNotAttack(GameTestHelper h) {
  var p=player(h);var a=predator(h);
  h.assertTrue(gate(a,p).isEmpty(),"First approach must warn");
  h.assertTrue(!a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Warning must not mark player as a threat");
  h.assertTrue(gate(a,p).isEmpty(),"Repeated evaluation must not bypass warning");
  h.runAfterDelay(41,()->{h.assertTrue(gate(a,p).orElse(null)==p,"Attack must be allowed after warning");p.discard();h.succeed();});
 }
 @GameTest(template="arena")
 public static void leavingDuringWarningPacifies(GameTestHelper h) {
  var p=player(h);var a=predator(h);gate(a,p);p.setPos(a.getX()+20,a.getY(),a.getZ());
  WaryPredatorsHooks.tickBreakOff(a);
  h.assertTrue(a.getBrain().hasMemoryValue(MemoryModuleType.PACIFIED),"Withdrawal during warning must end encounter");
  h.assertTrue(!a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"No attack on withdrawing player");p.discard();h.succeed();
 }
 @GameTest(template="arena")
 public static void withdrawalEndsHunt(GameTestHelper h) {
  var p=player(h);var a=predator(h);target(a,p);p.setPos(a.getX()+25,a.getY(),a.getZ());
  WaryPredatorsHooks.tickBreakOff(a);
  h.assertTrue(a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Withdrawal must wait for duration");
  h.runAfterDelay(41,()->{WaryPredatorsHooks.tickBreakOff(a);h.assertTrue(!a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Distant unprovoked hunt must end");h.assertTrue(a.getBrain().hasMemoryValue(MemoryModuleType.PACIFIED),"Disengagement must prevent immediate reacquisition");p.discard();h.succeed();});
 }
 @GameTest(template="arena")
 public static void returningInsideResetsWithdrawal(GameTestHelper h) {
  var p=player(h);var a=predator(h);target(a,p);p.setPos(a.getX()+25,a.getY(),a.getZ());WaryPredatorsHooks.tickBreakOff(a);
  h.runAfterDelay(25,()->{p.setPos(a.getX()+1,a.getY(),a.getZ());WaryPredatorsHooks.tickBreakOff(a);p.setPos(a.getX()+25,a.getY(),a.getZ());WaryPredatorsHooks.tickBreakOff(a);});
  h.runAfterDelay(45,()->{WaryPredatorsHooks.tickBreakOff(a);h.assertTrue(a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Returning inside must reset withdrawal timer");p.discard();h.succeed();});
 }
 @GameTest(template="arena")
 public static void nonPlayerHuntingUnaffected(GameTestHelper h) {
  var a=predator(h);var prey=h.spawn(EntityType.RABBIT,6,1,4);target(a,prey);
  WaryPredatorsHooks.tickBreakOff(a);h.assertTrue(a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==prey,"Non-player hunts must be preserved");h.succeed();
 }
 @GameTest(template="arena")
 public static void wolfHitTargetsWolfNotOwner(GameTestHelper h) {
  var p=player(h);var a=predator(h);gate(a,p);var wolf=h.spawn(EntityType.WOLF,6,1,4);wolf.tame(p);
  h.assertTrue(a.hurt(h.getLevel().damageSources().mobAttack(wolf),1),"Wolf damage must succeed");
  WaryPredatorsHooks.tickBreakOff(a);
  h.assertTrue(a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==wolf,"Predator must fight attacking wolf, not owner");p.discard();h.succeed();
 }
 @GameTest(template="arena")
 public static void allConfiguredAnimalsLoadAndTick(GameTestHelper h) {
  String[] names={"grizzly_bear","black_bear","polar_bear","cougar","panther","lion","sabertooth","tiger","crocodile","wolf","hyena","direwolf","boar","moose","bison","wildebeest","cow","yak","musk_ox","goat","pig"};
  for(String name:names){var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("tfc:"+name));h.assertTrue(type!=EntityType.PIG,"TFC type must exist: "+name);h.spawn(type,6,1,6);}
  h.runAfterDelay(5,h::succeed);
 }
 @GameTest(template="arena")
 public static void companionIgnoresWarningAndProtectsAgainstAttack(GameTestHelper h) {
  if (!net.neoforged.fml.ModList.get().isLoaded("wolfcompanion")) { h.succeed(); return; }
  var p=player(h);var a=predator(h);var wolf=h.spawn(EntityType.WOLF,4,1,5);wolf.tame(p);
  try {
   var clazz=Class.forName("com.crowsambit.wolfcompanion.WolfCompanion$CompanionGoal");
   var ctor=clazz.getDeclaredConstructor(Wolf.class);ctor.setAccessible(true);var goal=ctor.newInstance(wolf);
   var method=clazz.getDeclaredMethod("findThreat",net.minecraft.world.entity.player.Player.class);method.setAccessible(true);
   gate(a,p);h.assertTrue(method.invoke(goal,p)!=a,"Audrey must not attack warning-only predator");
   target(a,p);h.assertTrue(method.invoke(goal,p)==a,"Audrey must recognize committed predator attack");
  }catch(Exception ex){throw new RuntimeException(ex);}
  p.discard();h.succeed();
 }
 @GameTest(template="arena")
 public static void disabledSystemPreservesTfc(GameTestHelper h) {
  var p=player(h);var a=predator(h);var config=com.crowsambit.warypredators.config.WaryPredatorsConfig.PREDATORS_ENABLED;
  boolean original=config.get();config.set(false);
  try {h.assertTrue(gate(a,p).orElse(null)==p,"Master off must bypass warning");target(a,p);p.setPos(a.getX()+30,a.getY(),a.getZ());WaryPredatorsHooks.tickBreakOff(a);h.assertTrue(a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Master off must preserve TFC targets");}
  finally {config.set(original);p.discard();}h.succeed();
 }
 @GameTest(template="arena")
 public static void retaliationOverridesCertainBreakOff(GameTestHelper h) {
  var p=player(h);var a=predator(h);var cfg=com.crowsambit.warypredators.config.WaryPredatorsConfig.forEntity(a.getType()).breakOffChance;
  double original=cfg.get();cfg.set(1.0);
  try {a.hurt(h.getLevel().damageSources().playerAttack(p),1);WaryPredatorsHooks.tickBreakOff(a);h.assertTrue(a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==p,"Even 100 percent break-off cannot cancel fresh retaliation");}
  finally {cfg.set(original);p.discard();}h.succeed();
 }
 @GameTest(template="arena",timeoutTicks=320)
 public static void retaliationExpiresThenWithdrawalWorks(GameTestHelper h) {
  var p=player(h);var a=predator(h);a.hurt(h.getLevel().damageSources().playerAttack(p),1);p.setPos(a.getX()+30,a.getY(),a.getZ());
  h.runAfterDelay(241,()->{WaryPredatorsHooks.tickBreakOff(a);h.assertTrue(a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Withdrawal countdown must start after retaliation expires");});
  h.runAfterDelay(282,()->{WaryPredatorsHooks.tickBreakOff(a);h.assertTrue(!a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Expired retaliation permits withdrawal");p.discard();h.succeed();});
 }

 @GameTest(template="arena")
 public static void realPredatorAiInjectionWarns(GameTestHelper h) {
  var p=player(h);var a=predator(h);
  a.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,p);
  h.assertTrue(net.dries007.tfc.common.entities.ai.predator.PredatorAi.getAttackTarget(a).isEmpty(),"Actual TFC acquisition method must be gated by warning mixin");
  h.runAfterDelay(41,()->{h.assertTrue(net.dries007.tfc.common.entities.ai.predator.PredatorAi.getAttackTarget(a).orElse(null)==p,"Actual TFC acquisition allows attack after warning");p.discard();h.succeed();});
 }
 @GameTest(template="arena")
 public static void projectileRetaliationAcrossAllPredators(GameTestHelper h) {
  var p=player(h);p.setPos(p.getX()+40,p.getY(),p.getZ());
  for(String name:new String[]{"grizzly_bear","black_bear","polar_bear","cougar","panther","lion","sabertooth","tiger","crocodile","wolf","hyena","direwolf"}) {
   var a=(Predator)h.spawn(BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("tfc:"+name)),5,1,4);a.setNoAi(true);
   var arrow=new Arrow(EntityType.ARROW,h.getLevel());arrow.setOwner(p);
   h.assertTrue(a.hurt(h.getLevel().damageSources().arrow(arrow,p),1),"Projectile damage accepted: "+name);
   h.assertTrue(a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==p,"Shooter targeted: "+name);
   WaryPredatorsHooks.tickBreakOff(a);h.assertTrue(a.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null)==p,"Retaliation preserved: "+name);
  }p.discard();h.succeed();
 }

 @GameTest(template="arena")
 public static void switchingPlayersDoesNotRestartWarning(GameTestHelper h) {
  var first=player(h);var second=player(h);var a=predator(h);
  h.assertTrue(gate(a,first).isEmpty(),"First player starts warning");
  h.assertTrue(gate(a,second).isEmpty(),"Another player must not replace active warning");
  h.runAfterDelay(41,()->{h.assertTrue(gate(a,first).orElse(null)==first,"Original warning must expire without restart");first.discard();second.discard();h.succeed();});
 }
 @GameTest(template="arena")
 public static void rejectedHitDoesNotProvoke(GameTestHelper h) {
  var p=player(h);var a=predator(h);a.setInvulnerable(true);
  var arrow=new Arrow(EntityType.ARROW,h.getLevel());arrow.setOwner(p);
  h.assertTrue(!a.hurt(h.getLevel().damageSources().arrow(arrow,p),1),"Invulnerable animal rejects damage");
  h.assertTrue(!a.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET),"Rejected damage must not assign retaliation");p.discard();h.succeed();
 }

}
