package test.spring;

public class SpringBean {

    private Injectable injectable;
    
    public void setInjectable(Injectable inj) {
        injectable = inj;
    }
    
    public void init() {     
    }
    
    public void destroy() {      
    }
}
