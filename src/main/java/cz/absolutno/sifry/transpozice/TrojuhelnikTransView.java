package cz.absolutno.sifry.transpozice;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.FloatMath;
import cz.absolutno.sifry.Utils;

public final class TrojuhelnikTransView extends Trans2DView {
	
	private static final float v32 = (float)Math.sqrt(3)/2;
	private int row;

	public TrojuhelnikTransView(Context ctx, AttributeSet as) {
		super(ctx, as);
	}
	
	@Override
	public void setText(String s) {
		super.setText(s);
		row = Utils.ceil(((float)Math.sqrt(1+8*getLength())-1)/2);
		clearTrf();
	}
	
	@Override
	protected RectF getBounds() {
		return new RectF(-row/2, -0.5f, row/2, v32*row-0.5f);
	}
	
	@Override
	protected PointF get2DCoords(int i) {
		int y = Utils.floor(((float)Math.sqrt(1+8*i)-1)/2);
		int x = i - y*(y+1)/2;
		return new PointF(x-y/2f, y*v32);
	}
	
	@Override
	protected float getSuggestedTextSize() {
		return Math.min(Math.min(getLineLength(.5f, v32), getLineLength(.5f, -v32)), getLineLength(1, 0)); 
	}

}