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

package rabinizer.automata.nxt;

import jhoafparser.consumer.HOAConsumer;
import jhoafparser.consumer.HOAConsumerException;
import jhoafparser.consumer.HOAConsumerPrint;


import rabinizer.automata.IState;
import rabinizer.automata.output.HOAConsumerExtended;
import rabinizer.automata.output.RemoveComments;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

public class DetLimitAutomaton {

    public final @Nullable InitialComponent initialComponent;
    public final AcceptingComponent acceptingComponent;

    DetLimitAutomaton(@Nullable InitialComponent initialComponent, AcceptingComponent acceptingComponent) {
        this.initialComponent = initialComponent;
        this.acceptingComponent = acceptingComponent;
    }

    public Collection<String> getAlphabet() {
        return acceptingComponent.getAlphabet();
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
        HOAConsumerExtended<IState<?>> consumer = new HOAConsumerExtended<>(c, HOAConsumerExtended.AutomatonType.TRANSITION);
        IState<?> initialState = getInitialState();

        consumer.setHeader(initialState.toString(), getAlphabet());
        consumer.setGenBuchiAcceptance(acceptingComponent.acceptanceConditionSize);
        consumer.setInitialState(initialState);

        if (initialComponent != null) {
            initialComponent.toHOA(consumer);
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
