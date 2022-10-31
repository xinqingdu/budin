package indiv.budin.usercenter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author
 * @date 2022/10/31 21 36
 * discription
 */

@RunWith(SpringRunner.class)
@SpringBootTest(classes = RedisTest.class)
public class RedisTest {
    public RedisTest() {

    }
    public Logger logger = LoggerFactory.getLogger(RedisTest.class);
    @Autowired
    private RedisTemplate<String,String> stringRedisTemplate;
    @Test
    public void testRedis(){
        ValueOperations<String, String> stringStringValueOperations = stringRedisTemplate.opsForValue();
        try {
            stringStringValueOperations.set("dxq","student");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void testRedisApi(){

    }

}

