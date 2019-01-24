package com.sk89q.warmroast;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ClassMappingTest {

    @Test
    public void duplicateMethodMappings() throws IOException {
        File joinedFile = new File(getClass().getClassLoader().getResource("joined.srg").getFile());
        File methodsFile = new File(getClass().getClassLoader().getResource("methods.csv").getFile());
        McpMapping mapping = new McpMapping();
        mapping.read(joinedFile, methodsFile);

        
    }
}