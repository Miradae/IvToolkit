/*
 * Copyright 2017 Lukas Tenbrink
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ivorius.ivtoolkit.api;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Created by lukas on 26.01.17.
 */
public class ModRepresentation
{
    public String modID;

    public ModRepresentation(String modID)
    {
        this.modID = modID;
    }

    public static String id(Block block)
    {
        return name(block).toString();
    }

    public static ResourceLocation name(Block block)
    {
        return ForgeRegistries.BLOCKS.getKey(block);
    }

    public static String id(Item item)
    {
        return name(item).toString();
    }

    public static ResourceLocation name(Item item)
    {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    public boolean isLoaded()
    {
        return ModList.get().isLoaded(modID);
    }

    public String getID()
    {
        return modID;
    }

    public Block block(String id)
    {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(modID, id));
    }

    public Item item(String id)
    {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(modID, id));
    }
}