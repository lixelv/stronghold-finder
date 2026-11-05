package dev.strongholdfinder.xaero;

import java.util.Iterator;
import java.util.List;
import xaero.map.element.render.ElementRenderLocation;
import xaero.map.element.render.ElementRenderProvider;

final class StrongholdProvider extends ElementRenderProvider<MapMark, Object> {
    private Iterator<MapMark> marks = List.<MapMark>of().iterator();

    @Override
    public void begin(ElementRenderLocation location, Object context) {
        marks = List.<MapMark>of(new MapMark.Probe(0, 0)).iterator();
    }

    @Override
    public boolean hasNext(ElementRenderLocation location, Object context) {
        return marks.hasNext();
    }

    @Override
    public MapMark getNext(ElementRenderLocation location, Object context) {
        return marks.next();
    }

    @Override
    public void end(ElementRenderLocation location, Object context) {
        marks = List.<MapMark>of().iterator();
    }
}
