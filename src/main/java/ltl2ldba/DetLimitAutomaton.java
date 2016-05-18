/*
 * Copyright (C) 2016  (See AUTHORS)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ltl2ldba;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;
import rabinizer.automata.IState;
import rabinizer.automata.output.HOAConsumerGeneralisedBuchi;
import rabinizer.automata.output.RemoveComments;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DetLimitAutomaton {

    @Nullable
    public final InitialComponent initialComponent;
    public final AcceptingComponent acceptingComponent;

    DetLimitAutomaton(@Nullable InitialComponent initialComponent, AcceptingComponent acceptingComponent) {
        this.initialComponent = initialComponent;
        this.acceptingComponent = acceptingComponent;
    }

    public IState<?> getInitialState() {
        if (initialComponent != null) {
            return initialComponent.getInitialState();
        }

        return acceptingComponent.getInitialState();
    }

    public int size() {
        if (initialComponent == null) {
            return acceptingComponent.size();
        }

        return acceptingComponent.size() + initialComponent.size();
    }

    public void toHOA(HOAConsumer c) throws HOAConsumerException {
        HOAConsumerGeneralisedBuchi<IState<?>> consumer = new HOAConsumerGeneralisedBuchi<IState<?>>(c, acceptingComponent.getFactory(),
                acceptingComponent.acceptanceConditionSize);
        IState<?> initialState = getInitialState();

        consumer.setHOAHeader(initialState.toString());
        consumer.setInitialState(initialState);

        if (initialComponent != null) {
            initialComponent.toHOA(consumer);
        } else {
            acceptingComponent.generate();
        }

        acceptingComponent.toHOA(consumer);
        consumer.done();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean removeComments) {
        try (OutputStream stream = new ByteArrayOutputStream()) {
            HOAConsumer consumer = removeComments ? new RemoveComments(new HOAConsumerPrint(stream)) : new HOAConsumerPrint(stream);
            toHOA(consumer);
            return stream.toString();
        } catch (IOException | HOAConsumerException ex) {
            throw new IllegalStateException(ex.toString());
        }
    }
}
