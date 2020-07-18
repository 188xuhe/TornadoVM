package uk.ac.manchester.tornado.drivers.ptx.mm;

import uk.ac.manchester.tornado.api.exceptions.TornadoMemoryException;
import uk.ac.manchester.tornado.api.exceptions.TornadoOutOfMemoryException;
import uk.ac.manchester.tornado.api.mm.ObjectBuffer;
import uk.ac.manchester.tornado.runtime.common.RuntimeUtilities;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;

import static uk.ac.manchester.tornado.runtime.common.Tornado.*;

public class FieldBuffer {
    private final Field field;

    private final ObjectBuffer objectBuffer;

    public FieldBuffer(final Field field, final ObjectBuffer objectBuffer) {
        this.objectBuffer = objectBuffer;
        this.field = field;
    }

    public boolean isFinal() {
        return Modifier.isFinal(field.getModifiers());
    }

    public void allocate(final Object ref, long batchSize) throws TornadoOutOfMemoryException, TornadoMemoryException {
        objectBuffer.allocate(getFieldValue(ref), batchSize);
    }

    public int enqueueRead(final Object ref, final int[] events, boolean useDeps) {
        if (DEBUG) {
            trace("fieldBuffer: enqueueRead* - field=%s, parent=0x%x, child=0x%x", field, ref.hashCode(), getFieldValue(ref).hashCode());
        }
        // TODO: Offset 0
        int eventId = objectBuffer.enqueueRead(getFieldValue(ref), 0, (useDeps) ? events : null, useDeps);
        return (useDeps) ?  eventId : -1;
    }

    public List<Integer> enqueueWrite(final Object ref, final int[] events, boolean useDeps) {
        if (DEBUG) {
            trace("fieldBuffer: enqueueWrite* - field=%s, parent=0x%x, child=0x%x", field, ref.hashCode(), getFieldValue(ref).hashCode());
        }
        List<Integer> eventsIds = objectBuffer.enqueueWrite(getFieldValue(ref), 0, 0, (useDeps) ? events : null, useDeps);
        return (useDeps) ? eventsIds : Collections.emptyList();
    }

    public int getAlignment() {
        return objectBuffer.getAlignment();
    }

    public long getBufferOffset() {
        return objectBuffer.getBufferOffset();
    }

    private Object getFieldValue(final Object container) {
        Object value = null;
        try {
            value = field.get(container);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            warn("Illegal access to field: name=%s, object=0x%x", field.getName(), container.hashCode());
        }
        return value;
    }

    public boolean onDevice() {
        return objectBuffer.isValid();
    }

    public void read(final Object ref) {
        read(ref, null, false);
    }

    public int read(final Object ref, int[] events, boolean useDeps) {
        if (DEBUG) {
            debug("fieldBuffer: read - field=%s, parent=0x%x, child=0x%x", field, ref.hashCode(), getFieldValue(ref).hashCode());
        }
        // TODO: reading with offset != 0
        return objectBuffer.read(getFieldValue(ref), 0, events, useDeps);
    }

    public long toAbsoluteAddress() {
        return objectBuffer.toAbsoluteAddress();
    }

    public long toBuffer() {
        return objectBuffer.toBuffer();
    }

    public long toRelativeAddress() {
        return objectBuffer.toRelativeAddress();
    }

    public boolean needsWrite() {
        return !onDevice() || !RuntimeUtilities.isPrimitive(field.getType());
    }

    public void write(final Object ref) {
        if (DEBUG) {
            trace("fieldBuffer: write - field=%s, parent=0x%x, child=0x%x", field, ref.hashCode(), getFieldValue(ref).hashCode());
        }
        objectBuffer.write(getFieldValue(ref));
    }

    public String getFieldName() {
        return field.getName();
    }
}
