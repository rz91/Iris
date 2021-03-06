package de.baleipzig.iris.logic.converter.database;

import de.baleipzig.iris.common.Dimension;
import de.baleipzig.iris.common.utils.LayerUtils;
import de.baleipzig.iris.common.utils.NeuralNetCoreUtils;
import de.baleipzig.iris.enums.NeuralNetCoreType;
import de.baleipzig.iris.enums.FunctionType;
import de.baleipzig.iris.model.neuralnet.activationfunction.ActivationFunctionContainerFactory;
import de.baleipzig.iris.model.neuralnet.axon.Axon;
import de.baleipzig.iris.model.neuralnet.axon.IAxon;
import de.baleipzig.iris.model.neuralnet.layer.ILayer;
import de.baleipzig.iris.model.neuralnet.layer.Layer;
import de.baleipzig.iris.model.neuralnet.neuralnet.*;
import de.baleipzig.iris.model.neuralnet.node.INode;
import de.baleipzig.iris.model.neuralnet.node.Node;
import de.baleipzig.iris.persistence.entity.neuralnet.AxonEntity;
import de.baleipzig.iris.persistence.entity.neuralnet.LayerEntity;
import de.baleipzig.iris.persistence.entity.neuralnet.NeuralNetEntity;
import de.baleipzig.iris.persistence.entity.neuralnet.NodeEntity;

import java.util.*;
import java.util.stream.Collectors;

public class NeuralNetConverter {

    public static INeuralNetCore fromNeuralNetCoreEntity(NeuralNetEntity neuralNetEntity) {
        Map<Long, INode> allNodes = createAllNodesFromEntity(neuralNetEntity);

        createAxonsAndAddToNodes(neuralNetEntity, allNodes);

        int layerCount = neuralNetEntity.getLayers().size();
        ILayer inputLayer = fromLayerEntity(neuralNetEntity.getLayers().get(0), allNodes);

        List<ILayer> hiddenLayers = new ArrayList<>(layerCount-2);
        for(int count = 1; count < layerCount-1; count++){
            hiddenLayers.add(fromLayerEntity(neuralNetEntity.getLayers().get(count), allNodes));
        }

        ILayer outputLayer = fromLayerEntity(neuralNetEntity.getLayers().get(layerCount-1), allNodes);

        INeuralNetCore net = new NeuralNetCore();
        net.setInputLayer(inputLayer);
        hiddenLayers.forEach(net::addHiddenLayer);
        net.setOutputLayer(outputLayer);

        return net;
    }

    public static INeuralNetMetaData fromMetaDataEntity(NeuralNetEntity neuralNetEntity){

        INeuralNetMetaData metaData = new NeuralNetMetaData();

        metaData.setName(neuralNetEntity.getName());
        metaData.setDescription(neuralNetEntity.getDescription());
        metaData.setId(UUID.fromString(neuralNetEntity.getNeuralNetId()));
        return metaData;
    }

    public static Map<Long, INode> createAllNodesFromEntity(NeuralNetEntity neuralNetEntity) {

        Map<Long, INode> allNodes = new HashMap<>(neuralNetEntity.getNodes().size());

        neuralNetEntity.getNodes().forEach((nodeId, nodeEntity) -> {
            INode node = new Node();
            node.setBias(nodeEntity.getBias());
            node.setActivationFunctionContainer(ActivationFunctionContainerFactory.create(FunctionType.valueOf(nodeEntity.getActivationFunctionType())));
            allNodes.put(nodeId, node);
        });

        return allNodes;
    }

    public static void createAxonsAndAddToNodes(NeuralNetEntity neuralNetEntity, Map<Long, INode> allNodes) {
        neuralNetEntity.getAxons().forEach((key, axonEntity) -> {
            String[] parentAndChild = key.split(AxonEntity.PARENT_TO_CHILD_DELIMITER);

            INode parentNode = allNodes.get(Long.valueOf(parentAndChild[0]));
            INode childNode = allNodes.get(Long.valueOf(parentAndChild[1]));

            IAxon axon = new Axon();
            axon.setParentNode(parentNode);
            axon.setChildNode(childNode);
            axon.setWeight(axonEntity.getWeight());

            if(neuralNetEntity.getType().equals(NeuralNetCoreType.TRAIN.toString())){
                parentNode.addChildAxon(axon);
            }
            childNode.addParentAxon(axon);
        });
    }

    public static ILayer fromLayerEntity(LayerEntity layerEntity, Map<Long, INode> allNodes) {
        ILayer layer = new Layer();
        layer.resize(new Dimension(layerEntity.getDimX(), layerEntity.getDimY()));

        for (Long id : layerEntity.getNodes()) {
            layer.addNode(allNodes.get(id));
        }

        return layer;
    }

    public static NeuralNetEntity toNeuralNetEntity(INeuralNet neuralNet) {

        INeuralNetCore neuralNetCore = neuralNet.getNeuralNetCore();

        int totalNumberOfNodes = NeuralNetCoreUtils.getNumberOfNodes(neuralNetCore);

        Map<INode, Long> nodeIdMapper = getOrderedNodeIdMap(neuralNetCore);

        List<INode> orderedNodes = NeuralNetCoreUtils.getAllNodesLinewiseBottomToTop(neuralNetCore);

        Map<Long, NodeEntity> idNodeEntityMapper = new HashMap<>(totalNumberOfNodes);
        for(INode node : orderedNodes){
            idNodeEntityMapper.put(nodeIdMapper.get(node), toNodeEntity(node, nodeIdMapper));
        }

        Map<Integer, LayerEntity> layers = new HashMap<>(2+neuralNetCore.getHiddenLayers().size());
        int layerCount = 0;
        layers.put(layerCount++, toLayerEntity(neuralNetCore.getInputLayer(), nodeIdMapper));
        for(ILayer layer : neuralNetCore.getHiddenLayers()){
            layers.put(layerCount++, toLayerEntity(layer, nodeIdMapper));
        }
        layers.put(layerCount, toLayerEntity(neuralNetCore.getOutputLayer(), nodeIdMapper));

        Map<String,AxonEntity> axonEntities = getAxonEntities(orderedNodes, nodeIdMapper);

        NeuralNetEntity neuralNetEntity = new NeuralNetEntity();

        neuralNetEntity.setNeuralNetId(neuralNet.getNeuralNetMetaData().getId().toString());
        neuralNetEntity.setName(neuralNet.getNeuralNetMetaData().getName());
        neuralNetEntity.setDescription(neuralNet.getNeuralNetMetaData().getDescription());
        neuralNetEntity.setType(NeuralNetCoreUtils.getNeuralNetType(neuralNetCore).toString());
        neuralNetEntity.setNodes(idNodeEntityMapper);

        neuralNetEntity.setLayers(layers);
        neuralNetEntity.setAxons(axonEntities);

        return neuralNetEntity;
    }

    public static LayerEntity toLayerEntity(ILayer layer, Map<INode, Long> nodeIdMapper) {

        List<INode> orderedInputLayerNodes = LayerUtils.getAllNodesLinewise(layer);
        List<Long> nodes = new ArrayList<>(orderedInputLayerNodes.size());
        nodes.addAll(orderedInputLayerNodes.stream().map(nodeIdMapper::get).collect(Collectors.toList()));

        LayerEntity layerEntity = new LayerEntity();
        layerEntity.setNodes(nodes);
        layerEntity.setDimX(layer.getDim().getX());
        layerEntity.setDimY(layer.getDim().getY());

        return layerEntity;
    }

    public static NodeEntity toNodeEntity(INode node, Map<INode, Long> idMapper){

        NodeEntity nodeEntity = new NodeEntity();
        nodeEntity.setNodeId(idMapper.get(node));
        nodeEntity.setBias(node.getBias());
        FunctionType type = node.getActivationFunctionContainer() == null ? FunctionType.NONE : node.getActivationFunctionContainer().getFunctionType();

        nodeEntity.setActivationFunctionType(type.toString());

        return nodeEntity;
    }

    public static Map<String, AxonEntity> getAxonEntities(List<INode> orderedNodes, Map<INode, Long> nodeIdMapper) {

        Map<String, AxonEntity> axonEntities = new HashMap<>(getNumberOfParentAxons(orderedNodes));

        for(INode node : orderedNodes){
            for(IAxon axon : node.getParentAxons()){

                INode child = axon.getChildNode();
                INode parent = axon.getParentNode();

                AxonEntity axonEntity = new AxonEntity();
                axonEntity.setWeight(axon.getWeight());
                axonEntity.setChildNodeId(nodeIdMapper.get(child));
                axonEntity.setParentNodeId(nodeIdMapper.get(parent));

                String key = String.format("%s%s%s",nodeIdMapper.get(parent),AxonEntity.PARENT_TO_CHILD_DELIMITER, nodeIdMapper.get(child));
                axonEntities.put(key, axonEntity);
            }
        }

        return axonEntities;
    }

    private static Map<INode, Long> getOrderedNodeIdMap(INeuralNetCore net){

        Map<INode, Long> returnMap = new HashMap<>(NeuralNetCoreUtils.getNumberOfNodes(net));

        long index = 0;

        for(INode node : NeuralNetCoreUtils.getAllNodesLinewiseBottomToTop(net)){
            returnMap.put(node, index++);
        }

        return returnMap;
    }

    private static int getNumberOfParentAxons(List<INode> orderedNodes){

        int number = 0;

        for(INode node : orderedNodes){
            number += node.getParentAxons().size();
        }

        return number;
    }
}