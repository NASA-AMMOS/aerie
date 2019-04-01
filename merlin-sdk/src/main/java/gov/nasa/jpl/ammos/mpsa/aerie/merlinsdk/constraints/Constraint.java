package gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.constraints;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

public abstract class Constraint implements Serializable, PropertyChangeListener {

    private UUID id;
    protected String name;
    private String version;
    private String message;

    //you need to specify your own left and right leaf nodes as variables in your class
    //should we just have something here? e.g
    protected Constraint leftLeaf;
    protected Constraint rightLeaf;


    //keep a collection of listeners
    //this will be other nodes on the tree that are listening to us
    protected Set<PropertyChangeListener> treeNodeListeners;




    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) { this.message = message; }

    //These abstract functions are added because it is assumed that all constraint types will be implemented as trees
    //as a result, we expect there should be ways to traverse the trees (e.g. get the leaves, etc)
    //the implementation of this will be up to the user and what sort of constraint they are dealing with

    public abstract Object getLeft();

    public abstract Object getRight();

    public abstract Object getOperation();

    public abstract Object getValue();

    protected void addTreeNodeChangeListener(PropertyChangeListener newListener){
        treeNodeListeners.add(newListener);
    }

    protected void notifyTreeNodeListeners(boolean oldValue, boolean newValue){
        for (PropertyChangeListener name : treeNodeListeners){
            name.propertyChange(new PropertyChangeEvent(this, this.name, oldValue, newValue));
        }
    }

    public abstract void propertyChange(PropertyChangeEvent evt);

    public abstract void addListenersToChildren(Constraint condition);



}