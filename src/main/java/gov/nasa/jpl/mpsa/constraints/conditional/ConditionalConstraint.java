package gov.nasa.jpl.mpsa.constraints.conditional;
import gov.nasa.jpl.mpsa.constraints.Constraint;
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
    public Resource leftLeaf = null;
    private V rightLeaf = null;
    private String name = null;
    private String expr = null;

    //keep a collection of listeners
    //this will be other nodes on the tree that are listening to us
    private Set<PropertyChangeListener> treeNodeListeners;

    //how we will add other listeners to our listener set
    public void addTreeNodeChangeListener(PropertyChangeListener newListener){
        treeNodeListeners.add(newListener);
    }

    public void notifyTreeNodeListeners(boolean oldValue, boolean newValue){
        for (PropertyChangeListener name : treeNodeListeners){
            name.propertyChange(new PropertyChangeEvent(this, "ConditionalXpr", oldValue, newValue));
        }
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

    public String getName(){
        return this.name;
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
        evaluateNode();
        return this;
    }

    public ConditionalConstraint(String name){
        this.name = name;
    }

    public ConditionalConstraint logicXpr(ConditionalConstraint left, ConditionalConstraint right, String operation){
        this.left = left;
        this.right = right;
        this.operation = operation;
        build();
        return this;
    }

    public ConditionalConstraint logicXpr(Resource leftLeaf, V rightLeaf, String operation) {
        this.operation = operation;
        this.leftLeaf = leftLeaf;
        this.rightLeaf = rightLeaf;
        build();
        return this;
    }

    public void addListenersToChildren(ConditionalConstraint condition){
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

    //after evaluation, property change listeners will notify parents if value changes
    public void evaluateConstraintNodes(){

        assert(!isLeaf());
        assert(left!=null && right!=null);

        if (operation.equals("||")){
            this.value = (left.value || right.value);
        }

        else if (operation.equals("&&")){
            this.value = (left.value && right.value);
        }

        else {
            throw new IllegalArgumentException("Operation not recognized");
        }
    }

    public void evaluateNode() {
        if (isLeaf()) {
            evaluateLeafNodes();
        } else {
            evaluateConstraintNodes();
        }
    }

    public void evaluateLeafNodes(){

        assert(isLeaf());
        V leftVal = (V)leftLeaf.getCurrentValue();
        switch (operation)
        {
            case ">":
                if ((Double) leftVal > (Double) rightLeaf){ this.value = true;}
                else {this.value = false;}
                break;
            case ">=":
                if ((Double) leftVal >= (Double) rightLeaf){ this.value = true;}
                else {this.value = false;}
                break;
            case "==":
                if ((Double) leftVal == (Double) rightLeaf){ this.value = true;}
                else {this.value = false;}
                break;
            case "<=":
                if ((Double) leftVal <= (Double) rightLeaf){ this.value = true;}
                else {this.value = false;}
                break;
            case "<":
                if ((Double) leftVal < (Double) rightLeaf){ this.value = true;}
                else {this.value = false;}
                break;
            case "!=":
                if ((Double) leftVal != (Double) rightLeaf){ this.value = true;}
                else {this.value = false;}
                break;
            default:
                throw new IllegalArgumentException("Operation not recognized");

        }
        return;
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
        evaluateNode();

        //---delete later, for demo purposes--
        if (!isLeaf()) {
            updateConstraintNodeExpr();
            System.out.println(expr);
        }

        if (isLeaf()){
            updateLeafExpr();
            System.out.println(expr);
        }
        //---delete above--

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