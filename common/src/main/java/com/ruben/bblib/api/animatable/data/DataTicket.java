package com.ruben.bblib.api.animatable.data;

import java.util.Map;
import java.util.Objects;

public final class DataTicket<D> {

    private final String id;
    private final Class<? extends D> type;

    public DataTicket(String id, Class<? extends D> type) {
        this.id = id;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public Class<? extends D> type() {
        return type;
    }

    @SuppressWarnings("unchecked")
    public D getData(Map<? extends DataTicket<?>, ?> dataMap) {
        return (D) dataMap.get(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DataTicket<?> other)) {
            return false;
        }
        return Objects.equals(id, other.id) && Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
