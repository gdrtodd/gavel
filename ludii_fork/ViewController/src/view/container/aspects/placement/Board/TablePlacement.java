package view.container.aspects.placement.Board;

import java.util.List;

import bridge.Bridge;
import other.context.Context;
import other.topology.Vertex;
import util.ContainerUtil;
import view.container.aspects.placement.BoardPlacement;
import view.container.styles.BoardStyle;

/**
 * The placement of the pieces in the Table board.
 * 
 * @author Eric.Piette
 */
public class TablePlacement extends BoardPlacement
{
	/** The size of the home region of the board. */
	private final int homeSize;

	//-------------------------------------------------------------------------

	public TablePlacement(final Bridge bridge, final BoardStyle containerStyle)
	{
		super(bridge, containerStyle);
		homeSize = topology().vertices().size() / 4;
	}

	//-------------------------------------------------------------------------

	@Override
	public void customiseGraphElementLocations(final Context context)
	{
		final int pixels = placement.width;

		final int unitsX = 2 * homeSize() + 1 + 1; // 2 * homes + bar + border
		final int unitsY = 2 * (homeSize() - 1) + 1 + 1; // 2 * stacks + space + border

		final int mx = pixels / 2;
		final int my = pixels / 2;

		final int unit = pixels / unitsX / 2 * 2; // even
		final int border = unit / 2;

		final int ax = mx - (int) (unitsX * unit / 2.0 + 0.5);
		final int ay = my - (int) (unitsY * unit / 2.0 + 0.5);

		final int cx = ax + border;
		final int cy = ay + border;

		final List<Vertex> vertices = topology().vertices();
		final int halfSize = vertices.size() / 2;

		final int offset = (int) (0.08
				* Math.abs(vertices.get(0).centroid().getX() * pixels - vertices.get(1).centroid().getX() * pixels));

		for (int n = 0; n < vertices.size(); n++)
		{
			final Vertex vertex = vertices.get(n);

			final int sign = (n < halfSize) ? -1 : 1;

			// final int x = (int) (vertex.centroid().getX() * pixels);
			final int x = cx + (n % halfSize) * unit + unit / 2 + (leftSide(halfSize / 2, n) ? 0 : unit);
			final int y = cy + (n / halfSize * 10) * unit + unit / 2 + sign * offset;

			vertex.setCentroid(x / (double) pixels, y / (double) pixels, 0);
		}

		ContainerUtil.normaliseGraphElements(topology());
		ContainerUtil.centerGraphElements(topology());
		calculateCellRadius();
		resetPlacement(context);
	}
	
	//-------------------------------------------------------------------------

	public int homeSize()
	{
		return homeSize;
	}
	
	//-------------------------------------------------------------------------

	private static boolean leftSide(final int sideSize, final int index)
	{
		final int x = index / sideSize;
		return x % 2 == 0;
	}
	
	//-------------------------------------------------------------------------
}
