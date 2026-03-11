package com.ruben.bblib.api.animatable.data;

import com.ruben.bblib.api.model.data.ModelData;
import com.ruben.bblib.api.molang.MolangContext;
import net.minecraft.world.entity.Entity;

public final class DataTickets {

    public static final DataTicket<Entity> ENTITY = new DataTicket<>("entity", Entity.class);
    public static final DataTicket<ModelData> MODEL_DATA = new DataTicket<>("model_data", ModelData.class);
    public static final DataTicket<Double> TICK = new DataTicket<>("tick", Double.class);
    public static final DataTicket<Float> PARTIAL_TICK = new DataTicket<>("partial_tick", Float.class);
    public static final DataTicket<Boolean> MOVING = new DataTicket<>("moving", Boolean.class);
    public static final DataTicket<MolangContext> MOLANG_CONTEXT = new DataTicket<>("molang_context", MolangContext.class);
    public static final DataTicket<EntityRenderData> ENTITY_RENDER_DATA =
            new DataTicket<>("entity_render_data", EntityRenderData.class);

    private DataTickets() {
    }
}
