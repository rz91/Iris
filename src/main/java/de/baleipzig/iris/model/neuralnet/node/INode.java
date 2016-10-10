package de.baleipzig.iris.model.neuralnet.node;

import de.baleipzig.iris.model.neuralnet.axon.IAxon;
import java.util.List;
import java.util.function.DoubleFunction;

public interface INode extends Cloneable {

    List<IAxon> getParentAxons();
    void addParentAxon(IAxon axon);

    List<IAxon> getChildAxons();
    void addChildAxon(IAxon axon);

    double getError();
    void setError(double error);

    double getPreActivation();
    void setPreActivation(double preActivation);

    double getState();
    void setState(double state);

    double getBias();
    void setBias(double bias);

    DoubleFunction<Double> getActivationFunction();
}