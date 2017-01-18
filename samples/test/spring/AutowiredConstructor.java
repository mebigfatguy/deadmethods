package test.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AutowiredConstructor {

    private AutoDAO myDAO;
    @Autowired
    public AutowiredConstructor(AutoDAO dao) {
        myDAO = dao;
    }
    
    public interface AutoDAO {
    }
}
