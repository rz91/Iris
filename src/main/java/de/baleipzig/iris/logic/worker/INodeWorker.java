package de.baleipzig.iris.logic.worker;

import de.baleipzig.iris.model.neuralnet.node.INode;

public interface INodeWorker {

    void calculateState(INode node);
}
