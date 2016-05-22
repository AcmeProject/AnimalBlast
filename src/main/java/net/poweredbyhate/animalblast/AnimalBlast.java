package net.poweredbyhate.animalblast;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class AnimalBlast extends JavaPlugin implements Listener {

    private ArrayList<String> commandList = new ArrayList<>();
    AnimalBlast instance;

    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        for (String s : getConfig().getConfigurationSection("commands").getKeys(false)) {
            commandList.add(s);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent ev) {
        String cmd = ev.getMessage().toLowerCase().replace("/","");
        if (commandList.contains(cmd) && ev.getPlayer().hasPermission("animalblast."+cmd)) {
            Player p = ev.getPlayer();
            ev.setCancelled(true);
            if (p.hasMetadata("animalBlast.cooldown") && !p.hasPermission("cooldown.bypass")) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("CooldownMsg")));
                return;
            }
            Entity e = p.getWorld().spawnEntity(p.getLocation(), EntityType.valueOf(getConfig().getString("commands."+cmd+".entity")));
            e.setVelocity(p.getEyeLocation().getDirection().multiply(getConfig().getInt("Velocity")));
            p.getWorld().playSound(p.getLocation(), Sound.valueOf(getConfig().getString("LaunchSound")),1,1);
            doTheThing(ev.getPlayer(), e, getConfig().getString("commands."+cmd+".item") , getConfig().getString("commands."+cmd+".particle"));
            p.setMetadata("animalBlast.cooldown", new FixedMetadataValue(this,true)); //ty md_5 for telling me about metadata
            cook(e,1,1);
            cook(p,3,getConfig().getInt("Cooldown"));
        }
    }

    public void cook(final Entity ent, final int i, final int o) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (i==1 || i==0) {
                    ent.remove();
                }
                if (i==1 && getConfig().getBoolean("SpawnTNT")) {
                    ent.getWorld().spawn(ent.getLocation(), TNTPrimed.class);
                }
                if(i==3) {
                    ent.removeMetadata("animalBlast.cooldown", instance);
                }
            }
        }.runTaskLater(this, 20 * getConfig().getInt("KillTime") * o);
    }

    public void doTheThing(final Player p, final Entity entity, final String item, final String particle) {
        new BukkitRunnable() {
            @Override
            public void run() {
                p.getWorld().playEffect(entity.getLocation(), Effect.valueOf(particle), 1);
                for (int a = 0; a < getConfig().getInt("ItemCount"); a++) {
                    entity.setVelocity(entity.getVelocity().multiply(getConfig().getInt("Factor")));
                    Item i = p.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(Material.valueOf(item),1));
                    i.setPickupDelay(Integer.MAX_VALUE);
                    cook(i,0,1);
                }
                if (entity.isDead() || entity.isOnGround()) {
                    this.cancel();
                }
            }
        }.runTaskTimer(this, 0, 20 * getConfig().getInt("EffectTime"));
    }
}