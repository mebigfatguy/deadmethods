package test.serializable;

import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class IOClz implements Serializable {

    private static final long serialVersionUID = -8308067789551071759L;

    protected void writeObject(ObjectOutputStream oos) {
    }
    
    protected void readObject(ObjectInputStream ois) {
    }
    
    protected void writeExternal(ObjectOutput oo) {
    }
    
    protected void readExternal(ObjectInput oi) {
    }
}
