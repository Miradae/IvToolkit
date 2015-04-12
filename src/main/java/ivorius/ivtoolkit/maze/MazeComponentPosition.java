/*
 * Copyright 2014 Lukas Tenbrink
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

package ivorius.ivtoolkit.maze;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import ivorius.ivtoolkit.random.WeightedSelector;
import ivorius.ivtoolkit.tools.NBTCompoundObject;
import ivorius.ivtoolkit.tools.NBTTagCompounds;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandom;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by lukas on 20.06.14.
 */
public class MazeComponentPosition implements WeightedSelector.Item, NBTCompoundObject
{
    protected MazeComponent component;
    protected MazeRoom positionInMaze;

    public MazeComponentPosition()
    {
    }

    public MazeComponentPosition(MazeComponent component, MazeRoom positionInMaze)
    {
        this.component = component;
        this.positionInMaze = positionInMaze;
    }

    public MazeComponent getComponent()
    {
        return component;
    }

    public MazeRoom getPositionInMaze()
    {
        return positionInMaze;
    }

    @Override
    public double getWeight()
    {
        return component.getWeight();
    }

    public List<MazePath> getExitPaths()
    {
        return Lists.transform(component.getExitPaths(), new Function<MazePath, MazePath>()
        {
            @Nullable
            @Override
            public MazePath apply(MazePath path)
            {
                return path.add(positionInMaze);
            }
        });
    }

    public List<MazeRoom> getRooms()
    {
        return Lists.transform(component.getRooms(), new Function<MazeRoom, MazeRoom>()
        {
            @Nullable
            @Override
            public MazeRoom apply(MazeRoom room)
            {
                return room.add(positionInMaze);
            }
        });
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        component = NBTTagCompounds.read(compound.getCompoundTag("component"), MazeComponent.class);
        positionInMaze = new MazeRoom(compound.getIntArray("positionInMaze"));
    }

    @Override
    public void writeToNBT(NBTTagCompound compound)
    {
        compound.setTag("component", NBTTagCompounds.write(component));
        compound.setIntArray("positionInMaze", positionInMaze.coordinates);
    }
}
