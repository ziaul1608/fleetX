#!/usr/bin/env python3
import os
import sys
import subprocess

def find_java():
    candidates = [
        os.environ.get("JAVA_HOME", "") + "/bin/java",
        "/Applications/IntelliJ IDEA CE.app/Contents/jbr/Contents/Home/bin/java",
        "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java",
        "java"
    ]
    for c in candidates:
        if c and os.path.exists(c) and os.access(c, os.X_OK):
            return c
    return "java"

def main():
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    backend_dir = os.path.join(root_dir, "backend")
    classes_dir = os.path.join(backend_dir, "target", "classes")

    if not os.path.exists(classes_dir):
        print(f"Error: {classes_dir} not found. Please compile the backend first.")
        sys.exit(1)

    jar_names = [
        'spring-boot-starter-web-3.5.16.jar', 'spring-boot-starter-3.5.16.jar', 'spring-boot-3.5.16.jar',
        'spring-boot-autoconfigure-3.5.16.jar', 'spring-boot-starter-logging-3.5.16.jar', 'logback-classic-1.5.34.jar',
        'logback-core-1.5.34.jar', 'log4j-to-slf4j-2.24.3.jar', 'log4j-api-2.24.3.jar', 'jul-to-slf4j-2.0.18.jar',
        'jakarta.annotation-api-2.1.1.jar', 'snakeyaml-2.4.jar', 'spring-boot-starter-json-3.5.16.jar',
        'jackson-databind-2.21.4.jar', 'jackson-datatype-jdk8-2.21.4.jar', 'jackson-module-parameter-names-2.21.4.jar',
        'spring-boot-starter-tomcat-3.5.16.jar', 'tomcat-embed-core-10.1.55.jar', 'tomcat-embed-websocket-10.1.55.jar',
        'spring-web-6.2.19.jar', 'spring-beans-6.2.19.jar', 'micrometer-observation-1.15.12.jar',
        'micrometer-commons-1.15.12.jar', 'spring-webmvc-6.2.19.jar', 'spring-context-6.2.19.jar',
        'spring-expression-6.2.19.jar', 'spring-boot-starter-data-jpa-3.5.16.jar', 'spring-boot-starter-jdbc-3.5.16.jar',
        'HikariCP-6.3.3.jar', 'spring-jdbc-6.2.19.jar', 'hibernate-core-6.6.53.Final.jar', 'jakarta.persistence-api-3.1.0.jar',
        'jakarta.transaction-api-2.0.1.jar', 'jboss-logging-3.6.3.Final.jar', 'hibernate-commons-annotations-7.0.3.Final.jar',
        'jandex-3.2.0.jar', 'classmate-1.7.3.jar', 'byte-buddy-1.17.8.jar', 'jaxb-runtime-4.0.9.jar', 'jaxb-core-4.0.9.jar',
        'angus-activation-2.0.3.jar', 'txw2-4.0.9.jar', 'istack-commons-runtime-4.1.2.jar', 'jakarta.inject-api-2.0.1.jar',
        'antlr4-runtime-4.13.2.jar', 'spring-data-jpa-3.5.13.jar', 'spring-data-commons-3.5.13.jar', 'spring-orm-6.2.19.jar',
        'spring-tx-6.2.19.jar', 'slf4j-api-2.0.18.jar', 'spring-aspects-6.2.19.jar', 'aspectjweaver-1.9.25.1.jar',
        'spring-boot-starter-security-3.5.16.jar', 'spring-aop-6.2.19.jar', 'spring-security-config-6.5.11.jar',
        'spring-security-web-6.5.11.jar', 'spring-boot-starter-validation-3.5.16.jar', 'tomcat-embed-el-10.1.55.jar',
        'hibernate-validator-8.0.3.Final.jar', 'jakarta.validation-api-3.0.2.jar', 'spring-boot-starter-websocket-3.5.16.jar',
        'spring-messaging-6.2.19.jar', 'spring-websocket-6.2.19.jar', 'mysql-connector-j-9.7.0.jar',
        'flyway-core-11.7.2.jar', 'jackson-dataformat-toml-2.21.4.jar', 'jackson-core-2.21.4.jar',
        'jackson-datatype-jsr310-2.21.4.jar', 'jackson-annotations-2.21.jar', 'flyway-mysql-11.7.2.jar',
        'h2-2.3.232.jar', 'spring-core-6.2.19.jar', 'spring-jcl-6.2.19.jar', 'spring-security-core-6.5.11.jar',
        'spring-security-crypto-6.5.11.jar', 'jakarta.xml.bind-api-4.0.5.jar', 'jakarta.activation-api-2.1.4.jar'
    ]

    m2 = os.path.expanduser('~/.m2/repository')
    jar_map = {}
    for root, _, files in os.walk(m2):
        for f in files:
            if f in jar_names and f not in jar_map:
                jar_map[f] = os.path.join(root, f)

    cp = classes_dir + ':' + ':'.join(jar_map.values())
    java_bin = find_java()

    active_profile = os.environ.get("SPRING_PROFILES_ACTIVE", "dev")
    cmd = [
        java_bin,
        "-cp", cp,
        f"-Dspring.profiles.active={active_profile}",
        "com.fleetx.FleetxApplication"
    ]

    print(f"Starting FleetX Spring Boot Backend using {java_bin} on port 8080...")
    sys.stdout.flush()
    os.execv(java_bin, cmd)

if __name__ == "__main__":
    main()
