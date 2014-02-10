package test.serializable;

import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class IOClz implements Serializable {

    private static final long serialVersionUID = -8308067789551071759L;

    private void writeObject(ObjectOutputStream oos) {
    }
    
    private void readObject(ObjectInputStream ois) {
    }
    
    private void writeExternal(ObjectOutput oo) {
    }
    
    private void readExternal(ObjectInput oi) {
    }
}
