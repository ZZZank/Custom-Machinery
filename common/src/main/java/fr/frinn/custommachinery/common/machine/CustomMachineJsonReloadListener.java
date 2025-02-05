package fr.frinn.custommachinery.common.machine;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.architectury.platform.Platform;
import dev.architectury.utils.GameInstance;
import fr.frinn.custommachinery.CustomMachinery;
import fr.frinn.custommachinery.api.ICustomMachineryAPI;
import fr.frinn.custommachinery.common.util.CustomJsonReloadListener;
import fr.frinn.custommachinery.common.util.MachineList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.util.Map;

public class CustomMachineJsonReloadListener extends CustomJsonReloadListener {

    private static final String MAIN_PACKNAME = "main";

    public CustomMachineJsonReloadListener() {
        super("machines");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profiler) {
        ICustomMachineryAPI.INSTANCE.logger().info("Reading Custom Machinery Machines...");

        CustomMachinery.MACHINES.clear();

        map.forEach((id, json) -> {
            String packName;
            try {
                packName = resourceManager.getResourceOrThrow(new ResourceLocation(id.getNamespace(), "machines/" + id.getPath() + ".json")).sourcePackId();
            } catch (IOException e) {
                packName = MAIN_PACKNAME;
            }
            ICustomMachineryAPI.INSTANCE.logger().info("Parsing machine json: {} in datapack: {}", id, packName);

            if(!json.isJsonObject()) {
                ICustomMachineryAPI.INSTANCE.logger().error("Bad machine JSON: {} must be a json object and not an array or primitive, skipping...", id);
                return;
            }

            if(CustomMachinery.MACHINES.containsKey(id)) {
                ICustomMachineryAPI.INSTANCE.logger().error("A machine with id: {} already exists, skipping...", id);
                return;
            }

            DataResult<CustomMachine> result = CustomMachine.CODEC.read(JsonOps.INSTANCE, json);
            if(result.result().isPresent()) {
                CustomMachine machine = result.result().get();
                if(packName.equals(MAIN_PACKNAME))
                    machine.setLocation(MachineLocation.fromDefault(id));
                else
                    machine.setLocation(MachineLocation.fromDatapack(id, packName));
                CustomMachinery.MACHINES.put(id, machine);
                ICustomMachineryAPI.INSTANCE.logger().info("Successfully parsed machine json: {}", id);
                return;
            } else if(result.error().isPresent()) {
                ICustomMachineryAPI.INSTANCE.logger().error("Error while parsing machine json: {}, skipping...\n{}", id, result.error().get().message());
                return;
            }
            throw new IllegalStateException("No success nor error when parsing machine json: " + id + ". This can't happen.");
        });
        ICustomMachineryAPI.INSTANCE.logger().info("Finished creating custom machines.");

        if(GameInstance.getServer() != null)
            MachineList.setNeedRefresh();
    }
}