package gov.nasa.jpl.mpsa.constraints.conditional;
import gov.nasa.jpl.mpsa.constraints.Constraint;
import gov.nasa.jpl.mpsa.resources.Resource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;

public class ConditionalConstraint<V extends Comparable> extends Constraint {

    private ConditionalConstraint left;
    private ConditionalConstraint right;
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
        System.out.println("GOT HERE!!! In " + name);
        for (PropertyChangeListener name : treeNodeListeners){
            name.propertyChange(new PropertyChangeEvent(this, "ConditionalXpr", oldValue, newValue));
        }
    }

    public ConditionalConstraint(String name){
        this.name = name;
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


    public ConditionalConstraint logicXpr(Resource leftLeaf, V rightLeaf, String operation) {
        this.operation = operation;
        this.leftLeaf = leftLeaf;
        this.rightLeaf = rightLeaf;
        //this.leftLeaf.addChangeListener(this);
        addListenersToChildren(this);
        this.left = null;
        this.right = null;
        this.treeNodeListeners = new HashSet<>();

        evaluateLeafNodes();
        //updateLeafExpr();

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

    /*old
    public void addListenersByTreeTraversal(ConditionalConstraint condition){
        if (condition.left != null){
            addListenersByTreeTraversal(condition.left);
        }

        if (condition.right != null){
            addListenersByTreeTraversal(condition.right);
        }

        else {
            condition.leftLeaf.addChangeListener(this);
            return;
        }
    }
 */

    public ConditionalConstraint logicXpr(ConditionalConstraint left, ConditionalConstraint right, String operation){
        this.left = left;
        this.right = right;
        this.operation = operation;

        addListenersToChildren(this);
        evaluateConstraintNodes();

        this.treeNodeListeners = new HashSet<>();
        //updateConstraintNodeExpr();
        return this;
    }

    public void setConstraintValue(){
        boolean val = this.value;
        evaluateConstraintNodes();
        notifyTreeNodeListeners(val, this.value);
    }

    public void evaluateConstraintNodes(){

        if ((left != null) && (right != null)){
            if (operation.equals("||")){
                if (left.value || right.value){
                    this.value = true;
                }
                else {
                    this.value = false;
                }
            }

            if (operation.equals("&&")){
                if (left.value==true && right.value==true){
                    this.value = true;
                }
                else {
                    this.value = false;
                }
            }
        }
    }


    public void greaterThan(Double leftVal, Double rightLeaf){
        this.value = (leftVal > rightLeaf);
    }

    public void evaluateLeafNodes(){

        //if leaf nodes
        if ((leftLeaf != null) && (rightLeaf != null)){
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
            }
        }
        return;
    }


    @Override
    public void propertyChange(PropertyChangeEvent evt) {



        boolean oldvalue = value;

        System.out.println("name is " + name);

        System.out.println("value is " + this.leftLeaf.getCurrentValue());

        System.out.println("IN PROPERTY CHANGE!");
        evaluateLeafNodes();
        evaluateConstraintNodes();
        if (left != null) {
            updateConstraintNodeExpr();
            //System.out.println("Conditional value is " + this.value);
            System.out.println(expr);
        }

        if (leftLeaf != null){
            updateLeafExpr();
            //System.out.println("Resource expression is " + this.value + " resource expression is " + this.leftLeaf.getCurrentValue() + this.operation + this.rightLeaf);
            System.out.println(expr);
        }

        if (oldvalue != value){
            System.out.println("notifying listeners...");
            notifyTreeNodeListeners(oldvalue,value);
        }
    }
}