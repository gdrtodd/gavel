package app.display.dialogs.visual_editor.recs.utils;

public class Pair<R, S> {
    private final R r;
    private final S s;

    public Pair(R r, S s) {
        this.r = r;
        this.s = s;
    }
    public R getR() {
        return r;
    }

    public S getS() {
        return s;
    }

    @Override
	public String toString() {
        return r.toString() + " " + s.toString();
    }
}