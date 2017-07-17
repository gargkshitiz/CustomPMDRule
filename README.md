# CustomPMDRule

Steps to create a custome PMD rule:

1. Add this in the referring project pmd-ruleset file:  
`	<rule ref="rulesets/java/logging-custom.xml/CustomRuleConcatenationInLogStatement">
		<priority>1</priority>
	</rule>`
2. Add this in referring project pom file:   
`			<!--  PMD Plugin -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-pmd-plugin</artifactId>
				<version>3.7</version>
				<configuration>
					<targetJdk>1.7</targetJdk>
					<!-- to skip PMD check set "true" -->
          			<skip>false</skip>
					<!-- break the build on PMD violations, set "true" -->
					<failOnViolation>${failOnViolation}</failOnViolation>
					<skipPmdError>false</skipPmdError>
					<failurePriority>1</failurePriority>
					<verbose>true</verbose>
					<rulesets>
						<ruleset>${project.basedir}/src/main/resources/pmd-rulesets.xml</ruleset>
					</rulesets> 
				</configuration>
			    <dependencies>
	        	    <dependency>
						<groupId>${project.groupId}</groupId>
						<artifactId>kshitiz-pmd</artifactId>
						<version>${project.version}</version>
						<scope>compile</scope> 
					</dependency>
			    </dependencies>
				<executions>
					<execution>
						<phase>validate</phase>	
						<goals>
							<goal>check</goal>
						</goals>
					</execution>
				</executions>
			</plugin>`
3. Create a separate maven project kshitiz-pmd with files: PerformantLoggingRule.java, pom.xml and src\main\resources\rulesets\java\logging-custom.xml
