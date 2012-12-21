package com.forgenz.mobmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Chunk;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Keeps track of players within a given chunk </br>
 * Also keeps track of animals inside the chunk for limiting the number of animals that can be bread (Is That The Right Word?) in one place
 * 
 * @author Michael McKnight (ShadowDog007)
 *
 */
public class MMChunk
{
	private static Pattern layerPattern = Pattern.compile("^\\d+:{1}\\d+$");
	private static Pattern layerSplitPattern = Pattern.compile(":{1}");

	private Chunk chunk;
	private MMCoord coord;
	private ArrayList<MMLayer> layers;

	private int numPlayers = 0;
	private int numAnimals = 0;

	public MMChunk(final Chunk chunk)
	{
		// Fetches the layers from the config
		final List<String> layerList = P.cfg.getStringList("Layers");

		// Creates an array to store the layers
		layers = new ArrayList<MMLayer>(layerList.size());
		// Creates the layer objects
		for (final String layer : layerList)
		{
			// Checks the layer string is value
			if (!layerPattern.matcher(layer).matches())
				P.p.getLogger().warning(String.format("Found an invalid layer '%s'", layer));
			// Splits the range string for the layer
			final String[] range = layerSplitPattern.split(layer);

			// Converts range into integers
			int miny = Integer.valueOf(range[0]);
			int maxy = Integer.valueOf(range[1]);

			// Makes sure miny is actually the lower value
			if (maxy < miny)
			{
				miny = miny ^ maxy;
				maxy = miny ^ maxy;
				miny = miny ^ maxy;
			}

			layers.add(new MMLayer(miny, maxy, 0));

			// Adds any already existing Players or Animals
			for (final Entity entity : chunk.getEntities())
				if (entity instanceof Player)
				{
					playerEntered();
					for (final MMLayer layerIn : getLayersAt(entity.getLocation().getBlockY()))
						layerIn.playerEntered();
				} else if (entity instanceof Animals)
					++numAnimals;
		}

		this.chunk = chunk;
		coord = new MMCoord(chunk.getX(), chunk.getZ());
	}

	public Chunk getChunk()
	{
		return chunk;
	}

	public MMCoord getCoord()
	{
		return coord;
	}

	public MMLayer[] getLayers()
	{
		return layers.toArray(new MMLayer[0]);
	}

	public List<MMLayer> getLayersAt(final int y)
	{
		final ArrayList<MMLayer> layersAt = new ArrayList<MMLayer>();

		for (final MMLayer layer : layers)
			if (layer.insideRange(y))
				layersAt.add(layer);

		return layersAt;
	}

	public boolean withinBreedingLimits()
	{
		return numAnimals < P.cfg.getInt("BreedingMaximumsPerChunk." + chunk.getWorld().getName(), Integer.MAX_VALUE);
	}

	public void resetNumAnimals()
	{
		numAnimals = 0;
	}

	public void changeNumAnimals(final boolean increase)
	{
		if (increase)
			++numAnimals;
		else
			--numAnimals;
	}

	public boolean hasPlayers()
	{
		return numPlayers > 0;
	}

	public int getNumPlayers()
	{
		return numPlayers;
	}

	public void playerEntered()
	{
		++numPlayers;
		
		P.p.getLogger().warning(String.format("ENTERED Chunk %d, %d P: %d", chunk.getX(), chunk.getZ(), numPlayers));
	}

	public void playerLeft()
	{
		if (--numPlayers < 0)
		{
			numPlayers = 0;

			P.p.getLogger().warning("Player left a chunk with no players in it?");
			/*P.p.getLogger().warning(String.format("Chunk: %d, %d", chunk.getX(), chunk.getZ()));
			try
			{
				throw new Exception();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}*/
		}
		P.p.getLogger().warning(String.format("LEFT Chunk %d, %d P: %d", chunk.getX(), chunk.getZ(), numPlayers));
	}

	@Override
	public int hashCode()
	{
		return chunk.hashCode();
	}

	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final MMChunk other = (MMChunk) obj;
		if (coord == null)
		{
			if (other.coord != null)
				return false;
		} else if (!coord.equals(other.coord))
			return false;
		return true;
	}

}
