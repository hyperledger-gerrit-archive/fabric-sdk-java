package org.hyperledger.fabric.sdk;


import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by zerppen on 4/18/17.
 *
 * Test for Log4j configuration
 * src/resources/log4j.properties is the configuration file
 * of course we can choose JDK built-in logger
 */
public class TestLoggingSystem {
    //choose log4j logger
    public static Logger log4j = Logger.getLogger(TestLoggingSystem.class);

    //choose slf4j logger
    public static org.slf4j.Logger slf4j = LoggerFactory.getLogger(org.hyperledger.fabric.sdk.TestLoggingSystem.class);



    private String getTime(){

        //set time format
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date());
    }

    public  static void main(String [] args){

        TestLoggingSystem config = new TestLoggingSystem();
        System.out.println("*** We are testing method testError ***");
        config.testError();

        System.out.println("*** We are testing method testInfo ***");
        config.testInfo();

        System.out.println("*** We are testing method testDebug ***");
        config.testDebug();
    }

    /*
       1)log4j.properties' rootLogger is error,then console and all logs files under logs only show error logging
       2)log4j.properties' rootLogger is info,then console and all logs files under logs only show error/info logging
       3)log4j.properties' rootLogger is debug,then console and all logs files under logs  show all logging information
     */
    private void testError(){
        log4j.error(getTime()+" : It is method testError,logger is error");
        log4j.info(getTime()+" : It is method testError,logger is info ");
        log4j.debug(getTime()+" : It is method testError,logger is debug ");
    }

    private void testInfo(){
        log4j.error(getTime()+" : It is method testInfo,logger is error");
        log4j.info(getTime()+" : It is method testInfo,logger is info ");
        log4j.debug(getTime()+" : It is method testInfo,logger is debug ");
    }

    private void testDebug(){
        log4j.error(getTime()+" : It is method testDebug,logger is error");
        log4j.info(getTime()+" : It is method testDebug,logger is info ");
        log4j.debug(getTime()+" : It is method testDebug,logger is debug ");
    }
}
