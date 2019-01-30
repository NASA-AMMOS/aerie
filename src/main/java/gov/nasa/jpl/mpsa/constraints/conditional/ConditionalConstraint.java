package gov.nasa.jpl.mpsa.constraints.conditional;

import gov.nasa.jpl.mpsa.constraints.Constraint;
import gov.nasa.jpl.mpsa.resources.ArrayedResource;
import gov.nasa.jpl.mpsa.resources.Resource;

import java.beans.PropertyChangeEvent;

public class ConditionalConstraint<V extends Comparable> extends Constraint {

    private ConditionalConstraint left;
    private ConditionalConstraint right;
    private String operation;
    private Boolean value;
    private Resource leftLeaf = null;
    private V rightLeaf = null;
    private String name = null;
    private String expr = null;

    public ConditionalConstraint(){};

    public ConditionalConstraint(String name){
        this.name = name;
    }

    public void updateLeafExpr(){
        this.expr = this.name + ": " + this.leftLeaf.getName() + " (value " + this.leftLeaf.getCurrentValue() + ") " + operation + " "
                + this.rightLeaf + " evaluates to " + this.value;
    }

    public String getName(){
        return this.name;
    }

    public void updateConstraintNodeExpr(){
        this.expr = this.name + ": " + this.left.getName() + " " + this.operation + " "
                + this.right.getName() + " evaluates to " + this.value;
    }

    public ConditionalConstraint postfixXpr(Resource leftLeaf, V rightLeaf, String operation) {
        this.operation = operation;
        this.leftLeaf = leftLeaf;
        this.rightLeaf = rightLeaf;
        this.leftLeaf.addChangeListener(this);
        this.left = null;
        this.right = null;

        evaluateLeafNodes();
        //    updateLeafExpr();

        return this;
    }

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


    public ConditionalConstraint postfixXpr(ConditionalConstraint left, ConditionalConstraint right, String operation){
        this.left = left;
        this.right = right;
        this.operation = operation;

        addListenersByTreeTraversal(this);

        evaluateConstraintNodes();
        //   updateConstraintNodeExpr();

        return this;
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
      /*          System.out.println("in here!");
                System.out.println(left.name);
                System.out.println(left.value);
                System.out.println(right.name);
                System.out.println(right.value);*/
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
        if (leftVal > rightLeaf){ this.value = true;}
        else {this.value = false;}
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
    }
}