package ca.carbogen.korra.lavaflux;

import com.projectkorra.ProjectKorra.BendingPlayer;
import com.projectkorra.ProjectKorra.Methods;
import com.projectkorra.ProjectKorra.ProjectKorra;
import com.projectkorra.ProjectKorra.TempBlock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Carbogen on 2/8/2015.
 */
public class LavaFlux
{
	// Configurable Constants
	public final static int RANGE = ProjectKorra.plugin.getConfig().getInt("ExtraAbilities.Carbogen.LavaFlux.range");
	public final static int COOLDOWN = ProjectKorra.plugin.getConfig().getInt(
			"ExtraAbilities.Carbogen.LavaFlux.cooldown");
	public final static int DAMAGE = ProjectKorra.plugin.getConfig().getInt("ExtraAbilities.Carbogen.LavaFlux.damage");
	public final static int CLEANUP_DELAY = ProjectKorra.plugin.getConfig().getInt(
			"ExtraAbilities.Carbogen.LavaFlux.cleanupDelay");
	public final static boolean WAVE_ENABLED = ProjectKorra.plugin.getConfig().getBoolean(
			"ExtraAbilities.Carbogen.LavaFlux.waveEnabled");
	// Constants
	public final static double PARTICLE_DENSITY = 0.33;
	public final static int BLOCK_INTERVAL = 100;
	public final static int REVERT_INTERVAL = 200;
	public final static double PUSH_FACTOR = 0.5;
	public final static long PARTICLE_INTERVAL = 500;
	private static ConcurrentHashMap<Player, LavaFlux> instances = new ConcurrentHashMap<Player, LavaFlux>();
	// Instance Specific Constants
	private int range;
	private int cooldown;
	private int damage;
	private int cleanupdelay;

	// Instance Variables
	private Player player;
	private Vector direction;
	private Vector blockDirection;
	private boolean isSlapFinished = false;
	private boolean isWaveFinished = false;
	private boolean isReverting = false;
	private boolean areBlocksLoaded = false;
	private long lastProgressTime = 0;
	private long lastRevertTime = 0;
	private List<Block> centerLine = new ArrayList<Block>();
	private List<Block> centerLineWave = new ArrayList<Block>();
	private List<Block> rightLane = new ArrayList<Block>();
	private List<Block> leftLane = new ArrayList<Block>();
	private List<Block> affectedEarthBlocks = new ArrayList<Block>();
	private List<Block> affectedWaveBlocks = new ArrayList<Block>();
	private ListIterator<Block> aebli;
	private ListIterator<Block> awbli;
	private List<TempBlock> convertedWaveBlocks = new ArrayList<TempBlock>();
	private List<TempBlock> convertedEarthBlocks = new ArrayList<TempBlock>();
	private List<TempBlock> convertedLavaBlocks = new ArrayList<TempBlock>();

	public LavaFlux(Player p)
	{
		if (!isEligible(p))
			return;

		this.player = p;

		setRange(RANGE);
		setCooldown(COOLDOWN);
		setDamage(DAMAGE);
		setCleanupDelay(CLEANUP_DELAY);

		loadAffectedBlocks();
	}

	public static void progressAll()
	{
		for (Player p : instances.keySet())
			instances.get(p).progress();
	}

	public static void forceRemoveAll()
	{
		for (Player p : instances.keySet())
			instances.get(p).forceRemove();
	}

	public boolean isEligible(Player player)
	{
		final BendingPlayer bplayer = Methods.getBendingPlayer(player.getName());

		if (!Methods.canBend(player.getName(), "LavaFlux"))
			return false;

		if (Methods.getBoundAbility(player) == null)
			return false;

		if (!Methods.getBoundAbility(player).equalsIgnoreCase("LavaFlux"))
			return false;

		if (Methods.isRegionProtectedFromBuild(player, "LavaFlux", player.getLocation()))
			return false;

		if (!Methods.canLavabend(player))
			return false;

		if (bplayer.isOnCooldown("LavaFlux"))
			return false;

		return true;
	}

	public void loadAffectedBlocks()
	{
		if (areBlocksLoaded)
			return;

		direction = player.getEyeLocation().getDirection().setY(0).normalize();
		blockDirection = this.direction.clone().setX(Math.round(this.direction.getX()));
		blockDirection = blockDirection.setZ(Math.round(direction.getZ()));

		loadLine();
		loadSides();
		compileAffectedBlocks();
		reorderAffectedBlocks();

		aebli = affectedEarthBlocks.listIterator();
		awbli = affectedWaveBlocks.listIterator();

		Methods.getBendingPlayer(player.getName()).addCooldown("LavaFlux", getCooldown());

		instances.put(player, this);
	}

	public void loadLine()
	{
		BlockIterator bi = new BlockIterator(player.getWorld(),
				player.getLocation().add(0, -1, 0).add(blockDirection.multiply(2)).toVector(), direction, 0,
				getRange());

		while (bi.hasNext())
		{
			Block block = bi.next();

			while (!Methods.isEarthbendable(player, block))
			{
				block = block.getRelative(BlockFace.DOWN);
				if (Methods.isEarthbendable(player, block))
				{
					break;
				}
			}

			while (!Methods.isTransparentToEarthbending(player, block.getRelative(BlockFace.UP)))
			{
				block = block.getRelative(BlockFace.UP);
				if (Methods.isEarthbendable(player, block.getRelative(BlockFace.UP)))
				{
					break;
				}
			}

			if (Methods.isEarthbendable(player, block))
			{
				centerLine.add(block);
				centerLineWave.add(block.getRelative(BlockFace.UP));
			}
			else
				break;
		}
	}

//	public void reorderAffectedBlocks()
//	{
//		List<Block> reorderedBlocks = new ArrayList<Block>(affectedEarthBlocks);
//
//		for(int i = 1; i <= range; i++)
//			for(Block b : affectedEarthBlocks)
//				if(player.getLocation().distance(b.getLocation()) <= i)
//					if(!reorderedBlocks.contains(b))
//						reorderedBlocks.add(b);
//
//		affectedEarthBlocks = reorderedBlocks;
//	}

	public void loadSides()
	{
		for (Block b : centerLine)
		{
			Block left = b.getRelative(getLeftBlockFace(Methods.getCardinalDirection(blockDirection)), 1);
			Block right = b.getRelative(
					getLeftBlockFace(Methods.getCardinalDirection(blockDirection)).getOppositeFace(), 1);

			while (!Methods.isEarthbendable(player, left))
			{
				left = left.getRelative(BlockFace.DOWN);
				if (Methods.isEarthbendable(player, left))
				{
					break;
				}
			}

			while (!Methods.isTransparentToEarthbending(player, left.getRelative(BlockFace.UP)))
			{
				left = left.getRelative(BlockFace.UP);
				if (Methods.isEarthbendable(player, left.getRelative(BlockFace.UP)))
				{
					break;
				}
			}

			if (Methods.isEarthbendable(player, left))
			{
				leftLane.add(left);
				affectedWaveBlocks.add(left.getRelative(BlockFace.UP));
			}

			while (!Methods.isEarthbendable(player, right))
			{
				right = right.getRelative(BlockFace.DOWN);
				if (Methods.isEarthbendable(player, right))
				{
					break;
				}
			}

			while (!Methods.isTransparentToEarthbending(player, right.getRelative(BlockFace.UP)))
			{
				right = right.getRelative(BlockFace.UP);
				if (Methods.isEarthbendable(player, right.getRelative(BlockFace.UP)))
				{
					break;
				}
			}

			if (Methods.isEarthbendable(player, right))
			{
				rightLane.add(right);
				affectedWaveBlocks.add(right.getRelative(BlockFace.UP));
			}
		}
	}

	public void compileAffectedBlocks()
	{
		for (Block b : centerLine)
			affectedEarthBlocks.add(b);

		for (Block b : leftLane)
			affectedEarthBlocks.add(b);

		for (Block b : rightLane)
			affectedEarthBlocks.add(b);

		centerLine.clear();
		leftLane.clear();
		rightLane.clear();
	}

	public void reorderAffectedBlocks()
	{
		List<Block> abs = new ArrayList<Block>();

		for (int i = 3; i < range; i++)
		{
			for (int j = 0; j < affectedEarthBlocks.size(); j++)
			{
				Block b = affectedEarthBlocks.get(j);
				if (b.getLocation().distance(player.getLocation()) <= i)
				{
					if (b.getLocation().distance(player.getLocation()) >= 3)
					{
						abs.add(b);
						affectedEarthBlocks.remove(b);
					}
				}
			}
		}

		this.affectedEarthBlocks = abs;
	}

	public BlockFace getLeftBlockFace(BlockFace forward)
	{
		switch (forward)
		{
			case NORTH:
				return BlockFace.WEST;
			case SOUTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.SOUTH;
			case EAST:
				return BlockFace.NORTH;
			case NORTH_WEST:
				return BlockFace.SOUTH_WEST;
			case NORTH_EAST:
				return BlockFace.NORTH_WEST;
			case SOUTH_WEST:
				return BlockFace.SOUTH_EAST;
			case SOUTH_EAST:
				return BlockFace.NORTH_EAST;

			default:
				return BlockFace.NORTH;
		}
	}

	public void solidifyLava()
	{
		for (TempBlock tb : convertedEarthBlocks)
		{
			Block block = tb.getBlock();
			tb.revertBlock();

			convertedLavaBlocks.add(new TempBlock(block, Material.STONE, (byte) 0));
		}

		convertedEarthBlocks.clear();
	}

	public void revertStone()
	{
		isReverting = true;
	}

	public void createLava(Block b)
	{
		if (!convertedEarthBlocks.contains(b))
		{
			convertedEarthBlocks.add(new TempBlock(b, Material.STATIONARY_LAVA, (byte) 0));
		}
	}

	public void createWave(Block b)
	{
		if (!convertedWaveBlocks.contains(b))
		{
			convertedWaveBlocks.add(new TempBlock(b, Material.STATIONARY_LAVA, (byte) 0));
		}
	}

	public void removeWave()
	{
		for (TempBlock tb : convertedWaveBlocks)
			tb.revertBlock();

		convertedWaveBlocks.clear();
	}

	public void removeDistantWave(Block current)
	{
		for (TempBlock tb : convertedWaveBlocks)
		{
			if (tb.getLocation().distance(current.getLocation()) > 2)
				tb.revertBlock();
		}
	}

	public void affectFromWave(Block wave)
	{
		for (Entity e : Methods.getEntitiesAroundPoint(wave.getLocation(), 1.5))
		{
			if (e instanceof LivingEntity)
			{
				e.setFireTicks(100);
				Methods.damageEntity(player, e, getDamage());
				Methods.setVelocity(e, new Vector(0, -0.1, 0));
			}
		}
	}

	public void progress()
	{
		if (!player.isOnline() || player.isDead())
		{
			remove();
			return;
		}

		long time = System.currentTimeMillis();

		if (isReverting && isSlapFinished && isWaveFinished && time > lastRevertTime + REVERT_INTERVAL)
		{
			if (convertedLavaBlocks.size() == 0)
			{
				player.sendMessage("Done reverting!");
				instances.remove(player);
				return;
			}

			int randy = Methods.rand.nextInt(convertedLavaBlocks.size());

			convertedLavaBlocks.get(randy).revertBlock();
			convertedLavaBlocks.remove(randy);

			return;
		}

		if (isSlapFinished && isWaveFinished && time > lastProgressTime + cleanupdelay)
		{
			remove();
			return;
		}

		if (!isSlapFinished && time > lastProgressTime + BLOCK_INTERVAL)
		{
			lastProgressTime = time;

			isSlapFinished = true;

			Block block;

			for (int i = 0; i < 3; i++)
			{
				if (aebli.hasNext())
				{
					isSlapFinished = false;
					block = aebli.next();
				}

				else
					return;

				if (block != null)
				{
					if (!Methods.isRegionProtectedFromBuild(player, "LavaFlux", block.getLocation()))
					{
						if (Methods.isEarthbendable(player, block))
						{
							createLava(block);

							isWaveFinished = true;

							if (!WAVE_ENABLED)
								continue;

							Block wave;

							if (awbli.hasNext())
							{
								isWaveFinished = false;
								wave = awbli.next();
							}

							else
							{
								removeWave();
								return;
							}

							if (wave != null)
							{
								if (Methods.isTransparentToEarthbending(player, wave))
								{
									createWave(wave);

									removeDistantWave(wave);

									affectFromWave(wave);
								}
							}
						}
					}
				}
			}
		}
	}

	public void remove()
	{
		solidifyLava();
		player.sendMessage("Remove called!");

		Runnable r = new Runnable()
		{
			public void run()
			{
				revertStone();
				player.sendMessage("Reverting!");
			}
		};

		ProjectKorra.plugin.getServer().getScheduler().scheduleSyncDelayedTask(ProjectKorra.plugin, r, 40L);
	}

	public void forceRemove()
	{
		for (TempBlock tb : convertedEarthBlocks)
			tb.revertBlock();

		for (TempBlock tb : convertedLavaBlocks)
			tb.revertBlock();

		convertedEarthBlocks.clear();
		convertedLavaBlocks.clear();

		instances.remove(player);
	}

	// Instance Specific Constant Accessors
	public int getRange()
	{
		return this.range;
	}

	// Instance Specific Constant Mutators
	public void setRange(int newValue)
	{
		this.range = newValue;
	}

	public int getDamage()
	{
		return this.damage;
	}

	public void setDamage(int newValue)
	{
		this.damage = newValue;
	}

	public int getCooldown()
	{
		return this.cooldown;
	}

	public void setCooldown(int newValue)
	{
		this.cooldown = newValue;
	}

	public int getCleanupDelay()
	{
		return this.cleanupdelay;
	}

	public void setCleanupDelay(int newValue)
	{
		this.cleanupdelay = newValue;
	}
}
