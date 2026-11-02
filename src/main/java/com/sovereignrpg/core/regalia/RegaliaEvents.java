package com.sovereignrpg.core.regalia;

import com.google.common.collect.Multimap;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import top.theillusivec4.curios.api.event.CurioAttributeModifierEvent;

/** All regalia behavior: attribute effects, combat effects, XP, healing, potions and tooltips. */
@Mod.EventBusSubscriber(modid = "sovereignrpgcore")
public final class RegaliaEvents {
    private RegaliaEvents() {}

    private static int hand = Effects.bit(Effects.HAND), armor = Effects.bit(Effects.ARMOR), curio = Effects.bit(Effects.CURIO);

    // ---------- attribute based effects ----------

    private static void addAttr(Multimap<Attribute, AttributeModifier> out, ItemStack s, String curioSlot, int group, String slotName) {
        var list = RegaliaData.entries(s);
        if (list.isEmpty()) return;
        var cats = RegaliaData.cats(s, curioSlot);
        for (int i = 0; i < list.size(); i++) {
            Regalia r = Regalia.byId(list.get(i).id());
            if (r == null) continue;
            Regalia.Job j = r.jobFor(cats, group);
            if (j == null) continue;
            double v = j.value(list.get(i).tier());
            Attribute a = null;
            AttributeModifier.Operation op = AttributeModifier.Operation.MULTIPLY_BASE;
            switch (j.eff()) {
                case MOVE_SPEED -> { a = Attributes.MOVEMENT_SPEED; op = AttributeModifier.Operation.MULTIPLY_TOTAL; v /= 100; }
                case MAX_HEALTH -> { a = Attributes.MAX_HEALTH; op = AttributeModifier.Operation.ADDITION; }
                case SPELL_POWER -> { a = RegaliaData.attr("irons_spellbooks", "spell_power"); v /= 100; }
                case SPELL_CDR -> { a = RegaliaData.attr("irons_spellbooks", "cooldown_reduction"); v /= 100; }
                case STAMINA -> { a = RegaliaData.attr("epicfight", "staminar"); v /= 100; }
                default -> { }
            }
            if (a == null) continue;
            UUID id = UUID.nameUUIDFromBytes(("sovrpg:" + r.id + ":" + i + ":" + slotName).getBytes());
            out.put(a, new AttributeModifier(id, "sovereign_regalia_" + r.id, v, op));
        }
    }

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent e) {
        ItemStack s = e.getItemStack();
        EquipmentSlot sl = e.getSlotType();
        if (RegaliaData.entries(s).isEmpty()) return;
        com.google.common.collect.Multimap<Attribute, AttributeModifier> add = com.google.common.collect.HashMultimap.create();
        if (sl == EquipmentSlot.MAINHAND) addAttr(add, s, null, Effects.HAND, "main");
        else if (sl.getType() == EquipmentSlot.Type.ARMOR && s.getItem() instanceof ArmorItem a && a.getEquipmentSlot() == sl)
            addAttr(add, s, null, Effects.ARMOR, sl.getName());
        add.forEach(e::addModifier);
    }

    public static void registerCurios() {
        MinecraftForge.EVENT_BUS.register(CuriosHook.class);
    }

    public static final class CuriosHook {
        @SubscribeEvent
        public static void onCurio(CurioAttributeModifierEvent e) {
            addAttr(e.getModifiers(), e.getItemStack(), e.getSlotContext().identifier(), Effects.CURIO, "curio_" + e.getSlotContext().identifier() + e.getSlotContext().index());
        }
    }

    // ---------- combat ----------

    private static boolean isMagic(DamageSource src) {
        if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.DRAGON_BREATH)) return true;
        return src.getMsgId() != null && src.getMsgId().contains("irons_spellbooks");
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        DamageSource src = e.getSource();
        float amt = e.getAmount();
        Entity direct = src.getDirectEntity(), att = src.getEntity();

        if (att instanceof Player p && !p.level().isClientSide) {
            boolean proj = direct instanceof Projectile;
            boolean melee = !proj && direct == p;
            double mult = 1;
            if (proj) mult += Effects.sum(p, Eff.RANGED_DAMAGE, hand, Cat::ranged);
            float frac = victim.getHealth() / victim.getMaxHealth();
            if (frac <= 0.15f) mult += Effects.sum(p, Eff.LOW_HP_DAMAGE, hand);
            if (frac <= 0.10f) mult += Effects.sum(p, Eff.LOW_HP_DAMAGE_CURIOS, curio);
            amt *= (float) mult;
            if (melee || proj) {
                java.util.function.Predicate<Cat> pf = proj ? Cat::ranged : (Cat c) -> !c.ranged();
                double pierce = Effects.sum(p, Eff.ARMOR_PIERCE, hand, pf);
                if (pierce > 0) amt = pierce(victim, amt, Math.min(pierce, 0.9));
            }
            if (melee) {
                double ls = Effects.sum(p, Eff.LIFESTEAL, hand);
                if (ls > 0) p.heal((float) (amt * ls));
            }
        }

        if (victim instanceof Player v && !v.level().isClientSide) {
            double dr = Effects.sum(v, Eff.DR, armor);
            if (isMagic(src)) dr += Effects.sum(v, Eff.MAGIC_DR, armor | curio);
            if (att instanceof LivingEntity la && la.getHealth() / la.getMaxHealth() <= 0.15f) dr += Effects.sum(v, Eff.LOW_HP_DR, armor);
            if (dr > 0) amt *= (float) (1 - Math.min(dr, 0.8));
        }
        e.setAmount(amt);
    }

    /** Scales the damage so that armor takes off less of it: pierce 0.2 keeps 20% of what armor would have blocked. */
    private static float pierce(LivingEntity v, float amt, double pierce) {
        if (amt <= 0) return amt;
        float after = CombatRules.getDamageAfterAbsorb(amt, (float) v.getArmorValue(), (float) v.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        float f = after / amt;
        if (f <= 0 || f >= 1) return amt;
        return (float) (amt * (1 - (1 - f) * (1 - pierce)) / f);
    }

    @SubscribeEvent
    public static void onCrit(CriticalHitEvent e) {
        Player p = e.getEntity();
        double chance = Effects.sum(p, Eff.CRIT_CHANCE, hand);
        double bonus = Effects.sum(p, Eff.CRIT_DAMAGE, hand);
        boolean crit = e.isVanillaCritical() || e.getResult() == Event.Result.ALLOW;
        if (!crit && chance > 0 && p.getRandom().nextDouble() < chance) {
            e.setResult(Event.Result.ALLOW);
            e.setDamageModifier(1.5f);
            crit = true;
        }
        if (crit && bonus > 0) e.setDamageModifier((float) (e.getDamageModifier() + bonus));
    }

    // ---------- healing, xp, potions ----------

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (e.getEntity() instanceof Player p && !p.level().isClientSide) {
            double b = Effects.sum(p, Eff.HEAL_RECEIVED, curio);
            if (b > 0) e.setAmount((float) (e.getAmount() * (1 + b)));
        }
    }

    @SubscribeEvent
    public static void onXp(PlayerXpEvent.XpChange e) {
        if (e.getAmount() <= 0 || e.getEntity().level().isClientSide) return;
        double b = Effects.sum(e.getEntity(), Eff.XP_GAIN, curio);
        if (b > 0) e.setAmount((int) Math.round(e.getAmount() * (1 + b)));
    }

    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Added e) {
        if (!(e.getEntity() instanceof Player p) || p.level().isClientSide) return;
        MobEffectInstance inst = e.getEffectInstance();
        if (e.getOldEffectInstance() != null || !inst.getEffect().isBeneficial() || inst.isInfiniteDuration()) return;
        double b = Effects.sum(p, Eff.POTION_DURATION, armor | curio);
        if (b <= 0) return;
        try {
            ObfuscationReflectionHelper.setPrivateValue(MobEffectInstance.class, inst, (int) (inst.getDuration() * (1 + b)), "f_19503_");
        } catch (Throwable ignored) { }
    }

    // ---------- tooltips ----------

    private static Component col(String s, int rgb) {
        return Component.literal(s).withStyle(st -> st.withColor(TextColor.fromRgb(rgb)));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (s.getItem() instanceof RegaliaItem || !RegaliaData.socketable(s)) return;
        var list = RegaliaData.entries(s);
        int max = RegaliaData.vessels(s);
        var tip = e.getToolTip();
        tip.add(Component.empty());
        tip.add(col("Vessels (" + list.size() + "/" + max + ")", 0x5555FF));
        var cats = RegaliaData.cats(s, null);
        for (RegaliaData.Entry en : list) {
            Regalia r = Regalia.byId(en.id());
            if (r == null) continue;
            int t = Math.max(1, Math.min(3, en.tier()));
            tip.add(Component.empty().append(col("Regalia of " + r.name, r.color)).append(col(" (" + RegaliaItem.TIER_NAMES[t - 1] + ")", RegaliaItem.TIER_COLORS[t - 1])));
            Regalia.Job j = r.jobAny(cats);
            if (j != null) tip.add(col("  " + j.text(t), 0x55FF55));
        }
        for (int i = list.size(); i < max; i++) tip.add(col("Empty Vessel", 0x555555));
        if (s.getTag() != null && s.getTag().getBoolean("Unbreakable")) { /* vanilla already shows "Unbreakable" */ }
    }
}
