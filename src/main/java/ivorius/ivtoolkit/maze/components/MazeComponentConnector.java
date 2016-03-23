/*
 * Copyright 2015 Lukas Tenbrink
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

package ivorius.ivtoolkit.maze.components;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import ivorius.ivtoolkit.IvToolkitCoreContainer;
import ivorius.ivtoolkit.random.WeightedSelector;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Created by lukas on 15.04.15.
 */
public class MazeComponentConnector
{
    public static int INFINITE_REVERSES = -1;

    @Deprecated
    public static <M extends WeightedMazeComponent<C>, C> List<ShiftedMazeComponent<M, C>> randomlyConnect(MorphingMazeComponent<C> morphingComponent, List<M> components,
                                                                                                           ConnectionStrategy<C> connectionStrategy, final MazeComponentPlacementStrategy<M, C> placementStrategy, Random random)
    {
        return randomlyConnect(morphingComponent, components, connectionStrategy, new MazePredicate<M, C>()
        {
            @Override
            public boolean canPlace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<M, C> component)
            {
                return placementStrategy.canPlace(component);
            }

            @Override
            public void willPlace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<M, C> component)
            {

            }

            @Override
            public void didPlace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<M, C> component)
            {

            }

            @Override
            public void willUnplace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<M, C> component)
            {

            }

            @Override
            public void didUnplace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<M, C> component)
            {

            }

            @Override
            public boolean isDirtyConnection(MazeRoom dest, MazeRoom source, C c)
            {
                return placementStrategy.shouldContinue(dest, source, c);
            }
        }, random, 0);
    }

    public static <M extends WeightedMazeComponent<C>, C> List<ShiftedMazeComponent<M, C>> randomlyConnect(MorphingMazeComponent<C> maze, List<M> components,
                                                                                                           ConnectionStrategy<C> connectionStrategy, final MazePredicate<M, C> predicate, Random random, int reverses)
    {
        List<ReverseInfo<M, C>> placeOrder = new ArrayList<>();
        ReverseInfo<M, C> reversing = null;

        List<ShiftedMazeComponent<M, C>> result = new ArrayList<>();
        ArrayDeque<Triple<MazeRoom, MazeRoomConnection, C>> exitStack = new ArrayDeque<>();

        Predicate<ShiftedMazeComponent<M, C>> componentPredicate = Predicates.and(
                MazeComponents.compatibilityPredicate(maze, connectionStrategy),
                MazePredicates.placeable(maze, predicate));
        WeightedSelector.WeightFunction<ShiftedMazeComponent<M, C>> weightFunction = getWeightFunction();

        addAllExits(predicate, exitStack, maze.exits().entrySet());

        while (exitStack.size() > 0)
        {
            if (reversing == null)
            {
                if (maze.rooms().contains(exitStack.peekLast().getLeft()))
                {
                    exitStack.removeLast(); // Skip: Has been filled while queued
                    continue;
                }

                // Backing Up
                reversing = new ReverseInfo<>();
                reversing.exitStack = exitStack.clone();
                reversing.maze = maze.copy();
            }
            else
            {
                // Reversing
                predicate.willUnplace(maze, reversing.placed);

                exitStack = reversing.exitStack.clone(); // TODO Do a more efficient DIFF approach
                maze.set(reversing.maze); // TODO Do a more efficient DIFF approach

                predicate.didUnplace(maze, reversing.placed);

                result.remove(result.size() - 1);
            }

            Triple<MazeRoom, MazeRoomConnection, C> triple = exitStack.removeLast();
            MazeRoom room = triple.getLeft();
            MazeRoomConnection exit = triple.getMiddle();
            C connection = triple.getRight();

            List<ShiftedMazeComponent<M, C>> placeable = Lists.newArrayList(
                    FluentIterable.from(components)
                            .transformAndConcat(MazeComponents.<M, C>shiftAllFunction(exit, connection, connectionStrategy))
                            .filter(componentPredicate)
                            .toList()
            );

            if (reversing.triedIndices.size() > placeable.size())
                throw new RuntimeException("Maze component selection not static.");

            for (int i = 0; i < reversing.triedIndices.size(); i++)
                placeable.remove(reversing.triedIndices.get(i));

            if (placeable.size() == 0)
            {
                if (reverses == 0)
                {
                    IvToolkitCoreContainer.logger.warn("Did not find fitting component for maze!");
                    IvToolkitCoreContainer.logger.warn("Suggested: X with exits " + FluentIterable.from(maze.exits().entrySet()).filter(entryConnectsTo(room)));

                    reversing = null;
                }
                else
                {
                    if (reverses > 0) reverses--;

                    if (placeOrder.size() == 0)
                    {
                        IvToolkitCoreContainer.logger.warn("Maze is not completable!");
                        IvToolkitCoreContainer.logger.warn("Switching to flawed mode.");
                        reverses = 0;
                        reversing = null;
                    }
                    else
                        reversing = placeOrder.remove(placeOrder.size() - 1);
                }

                continue;
            }

            ShiftedMazeComponent<M, C> placing;
            if (WeightedSelector.canSelect(placeable, weightFunction))
            {
                placing = WeightedSelector.select(random, placeable, weightFunction);
                reversing.triedIndices.add(placeable.indexOf(placing));
            }
            else
            {
                int index = random.nextInt(placeable.size()); // All weight = 0 -> select at random
                placing = placeable.get(index);
                reversing.triedIndices.add(index);
            }
            reversing.placed = placing;

            // Placing
            predicate.willPlace(maze, placing);

            addAllExits(predicate, exitStack, placing.exits().entrySet());
            maze.add(placing);
            result.add(placing);

            predicate.didPlace(maze, placing);

            placeOrder.add(reversing);
            reversing = null;
        }

        return ImmutableList.<ShiftedMazeComponent<M, C>>builder().addAll(result).build();
    }

    private static Predicate<Map.Entry<MazeRoomConnection, ?>> entryConnectsTo(final MazeRoom finalRoom)
    {
        return input -> input != null && (input.getKey().has(finalRoom));
    }

    private static <M extends WeightedMazeComponent<C>, C> void addAllExits(MazePredicate<M, C> placementStrategy, Deque<Triple<MazeRoom, MazeRoomConnection, C>> exitStack, Set<Map.Entry<MazeRoomConnection, C>> entries)
    {
        for (Map.Entry<MazeRoomConnection, C> exit : entries)
        {
            MazeRoomConnection connection = exit.getKey();
            C c = exit.getValue();

            if (placementStrategy.isDirtyConnection(connection.getLeft(), connection.getRight(), c))
                exitStack.add(Triple.of(connection.getLeft(), connection, c));
            if (placementStrategy.isDirtyConnection(connection.getRight(), connection.getLeft(), c))
                exitStack.add(Triple.of(connection.getRight(), connection, c));
        }
    }

    private static <M extends WeightedMazeComponent<C>, C> WeightedSelector.WeightFunction<ShiftedMazeComponent<M, C>> getWeightFunction()
    {
        return item -> item.getComponent().getWeight();
    }

    private static class ReverseInfo<M extends WeightedMazeComponent<C>, C>
    {
        public final TIntList triedIndices = new TIntArrayList();

        public MorphingMazeComponent<C> maze;
        public ArrayDeque<Triple<MazeRoom, MazeRoomConnection, C>> exitStack;
        public ShiftedMazeComponent<M, C> placed;
    }
}
