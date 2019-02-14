package gov.nasa.jpl.mpsa.constraints.conditional;
import gov.nasa.jpl.mpsa.constraints.Constraint;
import gov.nasa.jpl.mpsa.engine.ConstraintEvaluationEngine;
import gov.nasa.jpl.mpsa.engine.Engine;
import gov.nasa.jpl.mpsa.resources.Resource;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

public class ConditionalConstraint<V extends Comparable> extends Constraint {

    private ConditionalConstraint left = null;
    private ConditionalConstraint right = null;
    private String operation;
    private Boolean value;
    private Resource leftLeaf = null;
    private V rightLeaf = null;
    private String expr = null;


    @Override
    public String getOperation(){
        return this.operation;
    }

    @Override
    public ConditionalConstraint getLeft(){
        return this.left;
    }

    @Override
    public ConditionalConstraint getRight(){
        return this.right;
    }

    @Override
    public Boolean getValue(){
        return this.value;
    }

    public Resource getLeftLeaf(){
        return this.leftLeaf;
    }

    public V getRightLeaf(){
        return this.rightLeaf;
    }

    public void updateLeafExpr(){
        this.expr = this.name + ": " + this.leftLeaf.getName() + " (value " + this.leftLeaf.getCurrentValue() + ") " + operation + " "
                + this.rightLeaf
                + " evaluates to " + this.value;
    }

    public void updateConstraintNodeExpr(){
        this.expr = this.name + ": " + this.left.getName() + " " + this.operation + " "
                + this.right.getName() + " evaluates to " + this.value;
    }

    public void forDemo(){
        if (!isLeaf()) {
            updateConstraintNodeExpr();
            System.out.println(expr);
        }

        if (isLeaf()){
            updateLeafExpr();
            System.out.println(expr);
        }
    }

    public boolean isLeaf(){
        return ((leftLeaf != null) && (rightLeaf != null));
    }

    public ConditionalConstraint withLeftLeaf(Resource leftLeaf){
        this.leftLeaf = leftLeaf;
        return this;
    }

    public ConditionalConstraint withRightLeaf(V rightLeaf){
        this.rightLeaf = rightLeaf;
        return this;
    }

    public ConditionalConstraint withLeftLeaf(ConditionalConstraint left){
        this.left = left;
        return this;
    }

    public ConditionalConstraint withRightLeaf(ConditionalConstraint right){
        this.right = right;
        return this;
    }

    public ConditionalConstraint withOperand(String operation){
        this.operation = operation;
        return logicXpr(leftLeaf, rightLeaf, operation);
    }

    public ConditionalConstraint build(){
        addListenersToChildren(this);
        this.treeNodeListeners = new HashSet<>();
        Engine conditionEvaluator = new ConstraintEvaluationEngine<>(this);
        conditionEvaluator.evaluateNode();
        this.value = conditionEvaluator.getResult();
        return this;
    }

    public ConditionalConstraint(String name){
        this.name = name;
    }


    public ConditionalConstraint logicXpr(Resource leftLeaf, V rightLeaf, String operation) {
        this.operation = operation;
        this.leftLeaf = leftLeaf;
        this.rightLeaf = rightLeaf;
        build();
        return this;
    }

    @Override
    public void addListenersToChildren(Constraint conditoinalConstraint){
        ConditionalConstraint condition = (ConditionalConstraint) conditoinalConstraint;
        if (condition.left != null){
            condition.right.addTreeNodeChangeListener(this);
        }

        if (condition.right != null){
            condition.right.addTreeNodeChangeListener(this);
        }

        if (condition.left == null && condition.right == null){
            condition.leftLeaf.addChangeListener(this);
        }
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        if (isLeaf()){
            if (evt.getOldValue().equals(evt.getNewValue())){
                System.out.println("No property change, change propagation stopped in " + name);
                return;
            }
        }

        boolean oldvalue = value;
        Engine conditionEvaluator = new ConstraintEvaluationEngine<>(this);
        conditionEvaluator.evaluateNode();
        this.value = conditionEvaluator.getResult();

        forDemo();

        if (oldvalue != value) {
            if (treeNodeListeners.size() > 0) {
                System.out.println(name + " used to be " + oldvalue + " and is notifying listeners it is now " + value + "...");
                notifyTreeNodeListeners(oldvalue, value);
            } else {
                System.out.println("no listeners, change propagation stopped. ");
            }
        }

        else {
            System.out.println("No property change, change propagation stopped in " + name);
        }
    }
}