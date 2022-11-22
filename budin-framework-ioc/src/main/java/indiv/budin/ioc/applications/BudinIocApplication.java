package indiv.budin.ioc.applications;

import indiv.budin.ioc.annotations.IocApplication;
import indiv.budin.ioc.constants.ExceptionMessage;
import indiv.budin.ioc.containers.AnnotationDependencyInjector;
import indiv.budin.ioc.containers.DependencyInjector;
import indiv.budin.ioc.containers.IocContainer;
import indiv.budin.ioc.exceptions.ApplicationRunException;
import indiv.budin.ioc.utils.PackageUtil;
import indiv.budin.ioc.utils.YamlUtil;
import indiv.budin.ioc.web.TomcatServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * @author
 * @date 2022/11/16 17 26
 * discription
 */
public class BudinIocApplication {
    static Logger logger = LoggerFactory.getLogger(BudinIocApplication.class);

    public static void run(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(IocApplication.class))
            throw new ApplicationRunException(ExceptionMessage.APPLICATION_RUN_EXCEPTION);
        String scanPath = clazz.getPackage().getName();
        Set<Class<?>> packageClass = PackageUtil.getPackageClass(scanPath);
        Map<String, Object> yamlMap = YamlUtil.getYamlMap(clazz, "application.yml");
        DependencyInjector annotationDependencyInjector = AnnotationDependencyInjector.creator().config(yamlMap).scan(packageClass).inject();

        Map tomcatConfig = YamlUtil.getObjectMapByPrefix(yamlMap, "budin.server");
        
        TomcatServer.create().config(tomcatConfig).build();
//        IocContainer iocContainer = annotationDependencyInjector.getIocContainer();
//        for (String key : iocContainer.getBeanContainer().keySet()) {
//            Object obj=iocContainer.getBean(key);
//            logger.info(obj.toString());
//            if (obj instanceof UserController){
//                UserController userController=(UserController) obj;
//                logger.info(userController.getUserService().toString());
//            }
//        }
    }

}