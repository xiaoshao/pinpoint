Pinpoint 类加载介绍
---
Pinpoint是当前APM非侵入采集的代表；Pinpoint实际上是一个非常强大的javaagent，对Pipoint中类加载原理进行理解有助于对整个Pinpoint非侵入式探针有一个全面的系统的认知。

Classloder 简介
---

JVM默认class loader
---
JVM默认有三个class loader， boot class loader， ext class loader 和App class loader， 
 1. boot class loader 是所有类加载器的根，在JVM启动时负责加载JVM内核（具体可以通过System.getProperty("sun.boot.class.path")查看具体加载哪些类文件）
  boot class loader 在系统中是不可获得的， 例如在通过其加载的类Sting.class.getClassLoader()时，可以看到获得的class loader 为null。 
  但是我们可以通过Java agent的入口参数Instrument接口提供的方法 appendToBootstrapClassLoaderSearch 向 boot class path中添加jar包，这样的这个jar中的类
  在被使用的时候就会被boot class loader 加载。
 
 2. ext class loader 负责加载 JVM扩展类 可以通过 System.getProperty("java.ext.dirs")查看具体加载的jar包
  ext class loader的parent是null，实际上是boot class loader。 
  可以通过ClassLoader.getSystemClassLoader().getParent().getParent()进行验证。
 
 3. App class loader 负责加载 class path下所有的类，可以通过 System.getProperty("java.class.path")查看具体加载的jar路径
    加载路径ClassLoader.getSystemClassLoader() 就是App class loader
    App class loader的parent是 ext class loader.
    ClassLoader.getSystemClassLoader().getParent()进行验证。

####Class Loader的双亲委托机制。
直接贴一段java.lang.ClassLoader的代码

```java

    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException
        {
            synchronized (getClassLoadingLock(name)) {
                // First, check if the class has already been loaded
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    long t0 = System.nanoTime();
                    try {
                        if (parent != null) {
                            c = parent.loadClass(name, false);
                        } else {
                            c = findBootstrapClassOrNull(name);
                        }
                    } catch (ClassNotFoundException e) {
                        // ClassNotFoundException thrown if class not found
                        // from the non-null parent class loader
                    }
    
                    if (c == null) {
                        // If still not found, then invoke findClass in order
                        // to find the class.
                        long t1 = System.nanoTime();
                        c = findClass(name);
    
                        // this is the defining class loader; record the stats
                        sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                        sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                        sun.misc.PerfCounter.getFindClasses().increment();
                    }
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }

```
从上面代码中很容易看出，首先在系统中查看该类是否已经加载，如果已经加载则直接返回，如果没有加载，则根据parent是否是null由parent加载还是bootstrap classloader加载，
如果没有加载到，则由自己加载。这个就是双亲委托机制。那么这么做的好处是什么呢？

请尝试思考一下，如果不是用双亲委托机制，当一个用户自己定义了一个 java.long.String 类的时候，并放在classpath路径下的时候，如果子classloader优先加载的时候，
jvm就会加载用户自定义的java.lang.String, 这里只是举了一个简单的例子，如果某些核心的类被别有用心的人劫持很可能发生意想不到的灾难。

## Thread Context classloader 简介
---
Thread Context classloader打破了双亲委托机制的限制，父classloader可以使用当前线程的classloader加载类，颠覆了父classloader不能使用子classloader或者没有
直接父子关系的classloader中加载类这种情况。在双亲委托机制的下，当 A 类使用了B类的时候， B 类必须在 A类的class loader及classloader parent 的classpath之内；
否则会报 Class Not Found Exception。

这么说可能比较抽象，以SPI （JDBC Driver）为例来进行说明；
这里有必要说明一下SPI（Service Provider Interface）， 常见的SPI主要有， JDBC， JCE， JNDI， JAXP， JBI等。这些SPI的接口都是由Java核心库来提供，而这些SPI具体
的实现确是由各个厂商实现，所以当我们在使用的时候，SPI的接口是由Bootstrap classloader来加载，而具体的实现确是由App classloader加载的。

下面以JDBC 来分析一下SPI类的加载过程，看完下面的介绍，相信大家会对classloader有一个新的认识。
```java

    Driver driver = Class.forName("com.mysql.jdbc.Driver").newInstance()
    
    Connection conn = driver.getConnection("jdbc:mysql://host:port/db");
```

这是以前没有使用SPI的一个Mysql JDBC链接的经典写法。 那么在Java6之后，我们就只需要把mysql-connector.jar加到classpath中之后，像下面这样来创建JDBC链接

```java
  Connection conn = java.sql.DriverManager.getConnection("jdbc:mysql://host:port/db")

```

z这个代码是如何加载到Mysql 的Driver的呢？我们就需要从DriverManager这个类入手了。

```java
public class DriverManager {
    // List of registered JDBC drivers
    private final static CopyOnWriteArrayList<DriverInfo> registeredDrivers = new CopyOnWriteArrayList<>();

    /* Prevent the DriverManager class from being instantiated. */
    private DriverManager(){}


    /**
     * Load the initial JDBC drivers by checking the System property
     * jdbc.properties and then use the {@code ServiceLoader} mechanism
     */
    static {
        loadInitialDrivers();
        println("JDBC DriverManager initialized");
    }
    
    private static void loadInitialDrivers() {
            String drivers;
            try {
                drivers = AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty("jdbc.drivers");
                    }
                });
            } catch (Exception ex) {
                drivers = null;
            }
            // If the driver is packaged as a Service Provider, load it.
            // Get all the drivers through the classloader
            // exposed as a java.sql.Driver.class service.
            // ServiceLoader.load() replaces the sun.misc.Providers()
    
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
    
                    ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
                    Iterator<Driver> driversIterator = loadedDrivers.iterator();
    
                    /* Load these drivers, so that they can be instantiated.
                     * It may be the case that the driver class may not be there
                     * i.e. there may be a packaged driver with the service class
                     * as implementation of java.sql.Driver but the actual class
                     * may be missing. In that case a java.util.ServiceConfigurationError
                     * will be thrown at runtime by the VM trying to locate
                     * and load the service.
                     *
                     * Adding a try catch block to catch those runtime errors
                     * if driver not available in classpath but it's
                     * packaged as service and that service is there in classpath.
                     */
                    try{
                        while(driversIterator.hasNext()) {
                            driversIterator.next();
                        }
                    } catch(Throwable t) {
                    // Do nothing
                    }
                    return null;
                }
            });
    
            println("DriverManager.initialize: jdbc.drivers = " + drivers);
    
            if (drivers == null || drivers.equals("")) {
                return;
            }
            String[] driversList = drivers.split(":");
            println("number of Drivers:" + driversList.length);
            for (String aDriver : driversList) {
                try {
                    println("DriverManager.Initialize: loading " + aDriver);
                    Class.forName(aDriver, true,
                            ClassLoader.getSystemClassLoader());
                } catch (Exception ex) {
                    println("DriverManager.Initialize: load failed: " + ex);
                }
            }
        }
        
        private static Connection getConnection(
                String url, java.util.Properties info, Class<?> caller) throws SQLException {
                /*
                 * When callerCl is null, we should check the application's
                 * (which is invoking this class indirectly)
                 * classloader, so that the JDBC driver class outside rt.jar
                 * can be loaded from here.
                 */
                ClassLoader callerCL = caller != null ? caller.getClassLoader() : null;
                synchronized(DriverManager.class) {
                    // synchronize loading of the correct classloader.
                    if (callerCL == null) {
                        callerCL = Thread.currentThread().getContextClassLoader();
                    }
                }
        
                if(url == null) {
                    throw new SQLException("The url cannot be null", "08001");
                }
        
                println("DriverManager.getConnection(\"" + url + "\")");
        
                // Walk through the loaded registeredDrivers attempting to make a connection.
                // Remember the first exception that gets raised so we can reraise it.
                SQLException reason = null;
        
                for(DriverInfo aDriver : registeredDrivers) {
                    // If the caller does not have permission to load the driver then
                    // skip it.
                    if(isDriverAllowed(aDriver.driver, callerCL)) {
                        try {
                            println("    trying " + aDriver.driver.getClass().getName());
                            Connection con = aDriver.driver.connect(url, info);
                            if (con != null) {
                                // Success!
                                println("getConnection returning " + aDriver.driver.getClass().getName());
                                return (con);
                            }
                        } catch (SQLException ex) {
                            if (reason == null) {
                                reason = ex;
                            }
                        }
        
                    } else {
                        println("    skipping: " + aDriver.getClass().getName());
                    }
        
                }
        
                // if we got here nobody could connect.
                if (reason != null)    {
                    println("getConnection failed: " + reason);
                    throw reason;
                }
        
                println("getConnection: no suitable driver found for "+ url);
                throw new SQLException("No suitable driver found for "+ url, "08001");
            }
}
```

魔法就在这个初始化静态代码块中，主要的加载过程集中在这一段代码中
```java
    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
    
                    ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);
                    Iterator<Driver> driversIterator = loadedDrivers.iterator();
    
                    /* Load these drivers, so that they can be instantiated.
                     * It may be the case that the driver class may not be there
                     * i.e. there may be a packaged driver with the service class
                     * as implementation of java.sql.Driver but the actual class
                     * may be missing. In that case a java.util.ServiceConfigurationError
                     * will be thrown at runtime by the VM trying to locate
                     * and load the service.
                     *
                     * Adding a try catch block to catch those runtime errors
                     * if driver not available in classpath but it's
                     * packaged as service and that service is there in classpath.
                     */
                    try{
                        while(driversIterator.hasNext()) {
                            driversIterator.next();
                        }
                    } catch(Throwable t) {
                    // Do nothing
                    }
                    return null;
                }
            });
```

首先大家先看一下ServiceLoader.load(Driver.class) 这个调用过程干了什么事情。

```java
    public static <S> ServiceLoader<S> load(Class<S> service) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ServiceLoader.load(service, cl);
    }
    
    public static <S> ServiceLoader<S> load(Class<S> service,
                                                ClassLoader loader)
        {
            return new ServiceLoader<>(service, loader);
        }
        
    private ServiceLoader(Class<S> svc, ClassLoader cl) {
        service = Objects.requireNonNull(svc, "Service interface cannot be null");
        loader = (cl == null) ? ClassLoader.getSystemClassLoader() : cl;
        acc = (System.getSecurityManager() != null) ? AccessController.getContext() : null;
        reload();
    }
    
    public void reload() {
        providers.clear();
        lookupIterator = new LazyIterator(service, loader);
    }

```

大家可以看到ServiceLoader的class loader就是线程上下文ClassLoader。还有大家要注意这个LazyIterator类，这个是魔法最后的堡垒。

在LazyIterator中有两个方法hasNextService 和NextService。
在hasNextService方法中有这样一行加载`configs = loader.getResources(fullName);`  （在用线程上线文类加载器读取资源路径 `META-INF/services/java.sql.Driver`）
这个资源内的内容。大家可以用Mysql connect jar包确认一下，是否里面包含了这样一个文件内容为（com.mysql.cj.jdbc.Driver）.

```java
    private boolean hasNextService() {
            if (nextName != null) {
                return true;
            }
            if (configs == null) {
                try {
                    String fullName = PREFIX + service.getName();
                    if (loader == null)
                        configs = ClassLoader.getSystemResources(fullName);
                    else
                        configs = loader.getResources(fullName);
                } catch (IOException x) {
                    fail(service, "Error locating configuration files", x);
                }
            }
            while ((pending == null) || !pending.hasNext()) {
                if (!configs.hasMoreElements()) {
                    return false;
                }
                pending = parse(service, configs.nextElement());
            }
            nextName = pending.next();
            return true;
        }

        private S nextService() {
            if (!hasNextService())
                throw new NoSuchElementException();
            String cn = nextName;
            nextName = null;
            Class<?> c = null;
            try {
                c = Class.forName(cn, false, loader);
            } catch (ClassNotFoundException x) {
                fail(service,
                     "Provider " + cn + " not found");
            }
            if (!service.isAssignableFrom(c)) {
                fail(service,
                     "Provider " + cn  + " not a subtype");
            }
            try {
                S p = service.cast(c.newInstance());
                providers.put(cn, p);
                return p;
            } catch (Throwable x) {
                fail(service,
                     "Provider " + cn + " could not be instantiated",
                     x);
            }
            throw new Error();          // This cannot happen
        }
```
在nextService中由于两行代码是要注意的`c = Class.forName(cn, false, loader); S p = service.cast(c.newInstance());` 其中第一行其实就是用线程上下文类加载器加载
在hasNext方法中读取到的文件的内容指定的类。而第二行就是在实例化具体的类。大家可以再到具体的驱动的实现中去看一下，会有一个静态初始块，调用DriverManager.registerDriver
将自己注册到DriverManager中，这样就能解释在DriverManager调用getConnection的可以使用classpath中具体厂商实现的Driver了。

## Pinpoint Class loader介绍
---
建议读者将上面的内容看懂之后，然后再结合Pinpoint代码读下面的内容，读完之后相信大家就已经完全掌握了JVM的类加载机制。

在介绍类加载之前有必要先介绍一下目录结构
```
├── boot
│   ├── pinpoint-annotations-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-bootstrap-core-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-bootstrap-core-optional-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-bootstrap-java9-1.8.1-SNAPSHOT.jar
│   └── pinpoint-commons-1.8.1-SNAPSHOT.jar
├── lib
│   ├── aopalliance-1.0.jar
│   ├── asm-6.1.jar
│   ├── asm-analysis-6.1.jar
│   ├── asm-commons-6.1.jar
│   ├── asm-tree-6.1.jar
│   ├── asm-util-6.1.jar
│   ├── guava-20.0.jar
│   ├── guice-4.1.0.jar
│   ├── javassist-3.22.0-GA.jar
│   ├── javax.inject-1.jar
│   ├── libthrift-0.10.0.jar
│   ├── log4j-1.2.16.jar
│   ├── log4j.xml
│   ├── netty-3.10.6.Final.jar
│   ├── pinpoint-profiler-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-profiler-optional-jdk6-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-profiler-optional-jdk7-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-profiler-optional-jdk8-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-profiler-optional-jdk9-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-profiler-test-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-rpc-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-thrift-1.8.1-SNAPSHOT.jar
│   ├── slf4j-api-1.7.21.jar
│   └── slf4j-log4j12-1.7.21.jar
├── log
│   └── pinpoint.agentId-pinpoint.log
├── pinpoint-bootstrap-1.8.1-SNAPSHOT.jar
├── pinpoint-real-env-lowoverhead-sample.config
├── pinpoint.config
├── plugin
│   ├── pinpoint-activemq-client-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-akka-http-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-arcus-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-cassandra-driver-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-commons-dbcp-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-commons-dbcp2-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-cubrid-jdbc-driver-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-cxf-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-dubbo-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-google-httpclient-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-gson-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-hikaricp-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-httpclient3-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-httpclient4-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-hystrix-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-ibatis-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-jackson-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-jboss-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-jdk-http-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-jetty-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-json-lib-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-jsp-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-jtds-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-kafka-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-log4j-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-logback-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-mariadb-jdbc-driver-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-mongodb-driver-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-mybatis-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-mysql-jdbc-driver-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-netty-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-ning-asynchttpclient-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-okhttp-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-oracle-jdbc-driver-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-php-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-postgresql-jdbc-driver-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-rabbitmq-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-redis-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-resin-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-resttemplate-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-rxjava-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-spring-boot-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-spring-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-thrift-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-tomcat-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-undertow-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-user-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-vertx-plugin-1.8.1-SNAPSHOT.jar
│   ├── pinpoint-weblogic-plugin-1.8.1-SNAPSHOT.jar
│   └── pinpoint-websphere-plugin-1.8.1-SNAPSHOT.jar
├── script
│   └── networktest.sh
└── tools
    └── pinpoint-tools-1.8.1-SNAPSHOT.jar
```

其中主要的目录有三个分别是boot， lib 和plugin，其中boot目录主要放Pinpoint的一些核心接口jar包， 而lib目录主要是用来存放pinpoint在启动过程中依赖到的某些jar包，
plugin文件夹中就是所有的添加拦截器的组件Plugin了。

在Pinpoint中自定义了两个类加载器，
 1. 在pinpoint1.6以及以前应该是PinpointURLClassLoader 1.8 中有多个与之相对应的ClassLoader，可以以ParallelClassLoader为例来进行分析。
 2. 在pinpoint1.6以及以前应该是ProfilerPluginClassLoader 在后面的版本中修改为ServerPluginLoader 和 JarPluginLoader 两个
 
 Pinpoint在启动之前首先会对主要的文件目录及文件进行检查。如果检查失败就会停止启动。
 在启动之前会将boot文件夹下的所有jar包通过Instrumentation.appendToBootstrapClassLoaderSearch添加到bootstrap class loader的classpath
 中，那么后面当有其他的类对boot路径下的类有依赖的时候，就有boot class loader进行加载。
 在初始化ICAgent的时候，
 
 其中PinpointURLClassLoader主要是用来加载DefaultAgent以及其依赖的，
 这个可以通过AgentBootLoader代码可以看出。
 ```java
    private Class<?> getBootStrapClass() {
        try {
            return this.classLoader.loadClass(bootClass);
        } catch (ClassNotFoundException e) {
            throw new BootStrapException("boot class not found. bootClass:" + bootClass + " Error:" + e.getMessage(), e);
        }
    }

```
其中的classloader就是PinpointURLClassLoader。使用PinpointURLClassLoader加载了DefaultAgent之后，那么DefaultAgent直接或者间接所
依赖的类都会有PinpointURLClassLoader加载，所以在DefaultAgent中创建的所有的Pinpoint探针组件都是使用PinpointURLClassLoader，包括PluginLoaderClassLoader。

至于为什么要使用executeTemplate初始化DefaultAgent是我一直没有想明白的地方，我在去掉修改context class loader之后，Pinpoint探针依然可以正常运行。
```java
    final Object agent = executeTemplate.execute(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Constructor<?> constructor = bootStrapClazz.getDeclaredConstructor(AgentOption.class);
                    return constructor.newInstance(agentOption);
                } catch (InstantiationException e) {
                    throw new BootStrapException("boot create failed. Error:" + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    throw new BootStrapException("boot method invoke failed. Error:" + e.getMessage(), e);
                }
            }
        });


    public class ContextClassLoaderExecuteTemplate<V> {
        // @Nullable
        private final ClassLoader classLoader;
    
        public ContextClassLoaderExecuteTemplate(ClassLoader classLoader) {
            this.classLoader = classLoader;
        }
    
        public V execute(Callable<V> callable) throws BootStrapException {
            try {
                final Thread currentThread = Thread.currentThread();
                final ClassLoader before = currentThread.getContextClassLoader();
                // ctxCl == null safe?
                currentThread.setContextClassLoader(this.classLoader);
                try {
                    return callable.call();
                } finally {
                    // even though  the "BEFORE" classloader  is null, rollback  is needed.
                    // if an exception occurs BEFORE callable.call(), the call flow can't reach here.
                    // so  rollback  here is right.
                    currentThread.setContextClassLoader(before);
                }
            } catch (BootStrapException ex){
                throw ex;
            } catch (Exception ex) {
                throw new BootStrapException("execute fail. Error:" + ex.getMessage(), ex);
            }
        }
    }

```

上面介绍了Pinpoint的整个所有组件是怎么加载起来的，下面我们介绍一下Pinpoint的Plugin是怎么加载的。
pinpoint 1.6版本的DefaultTraceMetadataLoaderService Pinpoint 1.8 版本以后的TraceMetadataLoaderServiceProvider中 会间接使用
PluginLoader读取com.navercorp.pinpoint.common.trace.TraceMetadataProvider中的内容后，利用PluginClassLoader加载该类，然后调用setup
方法注册相应的meta信息。然后在DefaultAgent加载Plugin的时候再使用PluginClassLoader读取com.navercorp.pinpoint.bootstrap.plugin.ProfilerPlugin
中的Plugin信息，利用PluginClassLoader加载其中的类，然后调用setup方法注册transform。
最后也是最经典的代码
```URLClassLoaderHandler.class

    @Override
    @SuppressWarnings("unchecked")
    public <T> Class<? extends T> injectClass(ClassLoader classLoader, String className) {
        try {
            if (classLoader instanceof URLClassLoader) {
                final URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
                addPluginURLIfAbsent(urlClassLoader);
                return (Class<T>) urlClassLoader.loadClass(className);
            }
        } catch (Exception e) {
            logger.warn("Failed to load plugin class {} with classLoader {}", className, classLoader, e);
            throw new PinpointException("Failed to load plugin class " + className + " with classLoader " + classLoader, e);
        }
        throw new PinpointException("invalid ClassLoader");
    }
    
    private void addPluginURLIfAbsent(URLClassLoader classLoader) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final URL[] urls = classLoader.getURLs();
        if (urls != null) {
            final boolean hasPluginJar = hasPluginJar(urls);
            if (!hasPluginJar) {
                if (isDebug) {
                    logger.debug("add Jar:{}", pluginURLString);
                }
                ADD_URL.invoke(classLoader, pluginURL);
            }
        }
    }
```

也就是在加载具体的类时， JVM会查找相应的ClassFileTransform，在做具体的注入之前，会调用URLClassLoaderHandler.injectClass, 这时就将相应plugin的jar包
添加到APPClassLoader的classpath中。

截止到目前为止Pinpoint的所有相关类都有了相应的类加载器加载，那么Pinpoint探针也就可以正常运行了。

## 总结
---
通过解析class cloader 的双亲委托机制和SPI的加载过程，我们可以在理论上对classloader有一个全面的认知，之后再通过解析Pinpoint代码，从实践方面
对Classloader进行了解析。 相信大家通过读完这篇文章之后会会对classloader有一个全面的深刻的理解。

如有不正确的地方欢迎大家拍砖。