/*
 * Copyright 2021 Andreas Forslund.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jassd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Java Automated Spring Source-code for Database Generator (JASSD-Gen)
 * <p>
 * Generates the boilerplate code for model, DAO interface classes, and Dao Implementation classes based on an existing database.
 * <p>
 * Configuration file specifies the target output directory and source database to connect to, as well as default spacing.
 * 
 * @author andreas.s.forslund@gmail.com
 */
public class GenerateClassesFromDatabase {

    private String serverName = "127.0.0.1";
    private int portNumber = 3306;
    private String dbName = "dbgentest";
    private String dbUser = "dbgenuser";
    private String dbPassword = "12345";
    private String dbType = "mysql";
    private String customConnectionString = "?characterEncoding=utf8&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    
    private String classPrefix = "Dbgentest";
    
    int spacing = 2;
    int curSpacing = 0;
    
    PrintWriter pw;
    
    class DatabaseValue {
        String databaseCol;
        String javaProp;
        String javaCapProp;
        String javaType;
        int dbType;
        boolean autoInc;
    }
    
    class TableValue {
        String databaseTable;
        String javaClass;
    }
    
    public void configureFromProperties(Properties p) {
        dbType = p.getProperty("database.type");
        dbName = p.getProperty("database.name");
        serverName = p.getProperty("database.server");
        portNumber = Integer.parseInt(p.getProperty("database.port"));
        dbUser = p.getProperty("database.user");
        dbPassword = p.getProperty("database.password");
        customConnectionString = p.getProperty("database.customConnectionString");
        
        classPrefix = p.getProperty("generator.basepackage");
        spacing = Integer.valueOf(p.getProperty("generator.spacing"));
    }
    
    public void setFileName(String directory, String filename) throws FileNotFoundException {
        closeCurrentFile();
        
        File dir = new File(directory);
        if (!dir.exists()) dir.mkdirs();
        
        File f = new File(directory + File.separator + filename);

        pw = new PrintWriter(f);
    }
    
    public void closeCurrentFile() {
        if(pw!=null) {
            pw.close();
        }
        pw = null;
    }
    
    public Connection getConnection() throws SQLException {
        Connection conn = null;

        String dbConnStr = "jdbc:"+dbType+"://"+serverName+":"+portNumber+"/"+dbName+customConnectionString;
        System.out.println("Db connection string: "+dbConnStr+", user:"+dbUser+", password:"+dbPassword);
        conn = DriverManager.getConnection(dbConnStr,dbUser,dbPassword);
        
        System.out.println("Connected to database");
        return conn;
    }
    
    public void genereteClassesFromDatabase() throws SQLException, FileNotFoundException {
        Connection c = getConnection();
        DatabaseMetaData dbMetaData = c.getMetaData();
        
        ResultSet rs = dbMetaData.getTables(null, null, null, new String[]{"TABLE"});
        
        List<String> tables = new ArrayList<String>();
        while(rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            tables.add(tableName);
            System.out.println("["+tableName+"]");
        }
        
        rs.close();
        
        Map<TableValue, List<DatabaseValue>> tableMap = new HashMap<TableValue, List<DatabaseValue>>();
        
        for(String table : tables) {
            System.out.println();
            System.out.println();
            System.out.println("*** Columns for "+table);
            System.out.println();
            
            String capTable = convertSqlNameToProperty(table);
            capTable = capTable.substring(0,1).toUpperCase()+capTable.substring(1);

            List<DatabaseValue> dbValues = new ArrayList<DatabaseValue>();
            
            rs = dbMetaData.getColumns(null, null, table, null);
            while(rs.next()) {
                String typeName = rs.getString("TYPE_NAME");
                String colName = rs.getString("COLUMN_NAME");
                String autoInc = rs.getString("IS_AUTOINCREMENT");
                
                String propName = convertSqlNameToProperty(colName);
                String capPropName = propName.substring(0,1).toUpperCase()+propName.substring(1);
                String javaType = "String";
                switch(typeName) {
                    case "VARCHAR":
                        javaType = "String";
                        break;
                    case "TEXT":
                        javaType = "String";
                        break;
                    case "INT":
                        javaType = "int";
                        break;
                    case "TINYINT":
                        javaType = "boolean";
                        break;
                    case "DOUBLE":
                        javaType = "double";
                        break;
                    case "DECIMAL":
                        javaType = "long";
                        break;
                    case "FLOAT":
                        javaType = "float";
                        break;
                    case "DATETIME":
                        javaType = "Date";
                        break;
                    default:
                        System.out.println("*******Unrecognized type: "+typeName);
                        
                }
                
                DatabaseValue dbValue = new DatabaseValue();
                dbValue.javaType = javaType;
                dbValue.databaseCol = colName;
                dbValue.javaProp = propName;
                dbValue.javaCapProp = capPropName;
                dbValue.dbType = rs.getInt("DATA_TYPE");
                dbValue.autoInc = autoInc.equals("YES");
                
                dbValues.add(dbValue);
            }
             
            TableValue tVal = new TableValue();
            tVal.databaseTable = table;
            tVal.javaClass = capTable;
            tableMap.put(tVal, dbValues);
            
            generateModelClass(capTable, dbValues);
        }
        
        println("");
        
        setFileName((classPrefix.toLowerCase()+".dao").replaceAll("\\.", File.separator), classPrefix+"Dao.java");        
        createDaoInterface(tableMap);
        
        setFileName((classPrefix.toLowerCase()+".dao.impl").replaceAll("\\.", File.separator), classPrefix+"DaoImpl.java");
        createDaoImpl(tableMap);
        
        setFileName((classPrefix.toLowerCase()+".controller").replaceAll("\\.", File.separator), classPrefix+"Controller.java");
        createTestController(tableMap);
        
        closeCurrentFile();
    }
    
    void createTestController(Map<TableValue, List<DatabaseValue>> tableMap) {
        curSpacing = 0;
        
        println("package "+classPrefix.toLowerCase()+".controller;");
        println();
        println("import "+classPrefix.toLowerCase()+".dao.*;");
        println("import "+classPrefix.toLowerCase()+".model.*;");
        println("import java.util.List;");
        println("import org.springframework.beans.factory.annotation.Autowired;");
        println("import org.springframework.stereotype.Controller;");
        println("import org.springframework.web.bind.annotation.PathVariable;");
        println("import org.springframework.web.bind.annotation.RequestMapping;");
        println("import org.springframework.web.bind.annotation.ResponseBody;");
        println("import org.springframework.web.bind.annotation.RestController;");
        println();
        println("@RestController");
        println("@RequestMapping(\"/"+classPrefix.toLowerCase()+"\")");
        println("public class "+classPrefix+"Controller {");
        println("@Autowired");
        println("private JassdDao jassdDao;");

        for(TableValue tVal : tableMap.keySet()) {
            List<DatabaseValue> dbVals = tableMap.get(tVal);
            
            String apiEndPoint = (tVal.javaClass+(tVal.javaClass.endsWith("s")?"":"s")).toLowerCase();
            String apiCall = tVal.javaClass+(tVal.javaClass.endsWith("s")?"":"s");
            println("@RequestMapping(\""+apiEndPoint+"\")");
            println("@ResponseBody");
            println("public List<"+tVal.javaClass+"> get"+apiCall+"() {");
            println("return "+classPrefix.toLowerCase()+"Dao.get"+apiCall+"();");
            println("}");
        }
        println("}");
    }

    void createDaoImpl(Map<TableValue, List<DatabaseValue>> tableMap) {
        
        curSpacing = 0;
        
        println("package "+classPrefix.toLowerCase()+".dao.impl;");
        println();
        println("import java.util.List;");
        println("import java.util.ArrayList;");
        println("import java.sql.Connection;");
        println("import java.sql.PreparedStatement;");
        println("import java.sql.SQLException;");
        println("import java.sql.Statement;");
        println("import java.util.Collections;");
        println("import java.util.Map;");
        println("import javax.sql.DataSource;");
        println("import org.springframework.beans.factory.annotation.Autowired;");
        println("import org.springframework.jdbc.core.PreparedStatementCreator;");
        println("import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;");
        println("import org.springframework.jdbc.support.GeneratedKeyHolder;");
        println("import org.springframework.jdbc.support.KeyHolder;");
        println("import java.sql.ResultSet;");
        println("import org.springframework.jdbc.core.JdbcTemplate;");
        println("import org.springframework.jdbc.core.RowMapper;");
        println("import org.springframework.stereotype.Component;");
        println("import "+classPrefix.toLowerCase()+".dao.*;");
        println("import "+classPrefix.toLowerCase()+".model.*;");
        println();
        println("@Component(\"JassdDao\")");
        println("public class "+classPrefix+"DaoImpl implements "+classPrefix+"Dao {");
        println();
        println("private JdbcTemplate jdbcTemplate;");
        println("private NamedParameterJdbcTemplate namedJdbcTemplate;");
        println();
        println("@Autowired");
        println("private void setDataSource(DataSource dataSource) {");
        println("jdbcTemplate = new JdbcTemplate(dataSource);");
        println("namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);");
        println("}");
        println();
        for(TableValue tVal : tableMap.keySet()) {
            List<DatabaseValue> dbVals = tableMap.get(tVal);
            
            println("@Override");
            println("public List<"+tVal.javaClass+"> get"+tVal.javaClass+(tVal.javaClass.endsWith("s")?"":"s")+"() {");
            println("return jdbcTemplate.query(\"SELECT * FROM "+tVal.databaseTable+"\", new "+tVal.javaClass+"Extractor());");
            println("}");
            println("@Override");
            println("public void create"+tVal.javaClass+"("+tVal.javaClass+" "+tVal.javaClass.toLowerCase()+") {");
            println("KeyHolder keyHolder = new GeneratedKeyHolder();");
            println("jdbcTemplate.update(new "+tVal.javaClass+"Creator("+tVal.javaClass.toLowerCase()+"), keyHolder);");
            for(DatabaseValue dbVal : dbVals) {
                if(dbVal.autoInc) {
                    println(""+tVal.javaClass.toLowerCase()+".set"+dbVal.javaCapProp+"(keyHolder.getKey().intValue());");
                    break;
                }
            }
            println("}");
            println("@Override");
            println("public void update"+tVal.javaClass+"("+tVal.javaClass+" "+tVal.javaClass.toLowerCase()+") {");
            println("jdbcTemplate.update(new "+tVal.javaClass+"Updater("+tVal.javaClass.toLowerCase()+"));");
            println("}");
            for(DatabaseValue dbVal : dbVals) {
                if(dbVal.autoInc) {
                    println("@Override");
                    println("public "+tVal.javaClass+" get"+tVal.javaClass+"ById(int "+dbVal.javaProp+") {");
                    println("Map namedParameters = Collections.singletonMap(\""+dbVal.javaProp+"\", "+dbVal.javaProp+");");
                    println("return namedJdbcTemplate.queryForObject(\"SELECT * FROM "+tVal.databaseTable+" WHERE "+dbVal.databaseCol+"=:"+dbVal.javaProp+"\", namedParameters, new "+tVal.javaClass+"Extractor());");
                    println("}");
                }
            }
            generateCreatorSubClass(tVal, dbVals);
            
            generateUpdaterSubClass(tVal, dbVals);
            
            generateExtractorSubClass(tVal, dbVals);
        }
        println("}");
    }

    void generateExtractorSubClass(TableValue tVal, List<DatabaseValue> dbVals) {
        println("private class "+tVal.javaClass+"Extractor implements RowMapper<"+tVal.javaClass+"> {");
        println("@Override");
        println("public "+tVal.javaClass+" mapRow(ResultSet rs, int i) throws SQLException {");
        String varName = (""+tVal.javaClass.charAt(0)).toLowerCase();
        println(tVal.javaClass+" "+varName+" = new "+tVal.javaClass+"();");
        for(DatabaseValue dbValue : dbVals) {
            println(varName+".set"+dbValue.javaCapProp+"(rs.get"+capitalize(dbValue.javaType)+"(\""+dbValue.databaseCol+"\"));");
        }
        println("return "+varName+";");
        println("}");
        println("}");
    }
    
    String capitalize(String s) {
        return s.substring(0,1).toUpperCase()+s.substring(1);
    }
    
    void generateUpdaterSubClass(TableValue tVal, List<DatabaseValue> dbVals) {
        println("private class "+tVal.javaClass+"Updater implements PreparedStatementCreator {");
        println("private "+tVal.javaClass+" "+tVal.javaClass.toLowerCase()+";");
        println("public "+tVal.javaClass+"Updater("+tVal.javaClass+" "+tVal.javaClass.toLowerCase()+") {");
        println("this."+tVal.javaClass.toLowerCase()+" = "+tVal.javaClass.toLowerCase()+";");
        println("}");
        println("@Override");
        println("public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {");
        println("PreparedStatement ps = connection.prepareStatement(");
        String updateStr = "      \"UPDATE "+tVal.databaseTable+" SET ";
        for(DatabaseValue dbVal : dbVals) {
            if(!dbVal.autoInc) {
                updateStr += dbVal.databaseCol+"=?, ";
            }
        }
        updateStr = updateStr.substring(0, updateStr.length()-2)+" WHERE ";
        for(DatabaseValue dbVal : dbVals) {
            if(dbVal.autoInc) {
                updateStr += dbVal.databaseCol+"=?\"";
            }
        }
        println(updateStr+");");
        
        int counter = 1;
        for(DatabaseValue dbVal : dbVals) {
            if(!dbVal.autoInc) {
                if(dbVal.javaType.equals("Date")) {
                    println("ps.setDate("+counter+", new java.sql.Date("+tVal.javaClass.toLowerCase()+".get"+dbVal.javaCapProp+"().getTime()));");
                    counter++;
                } else {
                    println("ps.set"+capitalize(dbVal.javaType)+"("+counter+", "+tVal.javaClass.toLowerCase()+".get"+dbVal.javaCapProp+"());");
                    counter++;                   
                }
            }
        }
        for(DatabaseValue dbVal : dbVals) {
            if(dbVal.autoInc) {
                println("ps.set"+capitalize(dbVal.javaType)+"("+counter+", "+tVal.javaClass.toLowerCase()+".get"+dbVal.javaCapProp+"());");
                counter++;
                break;
            }
        }
        println("return ps;");
        println("}");
        println("}");
    }

    void generateCreatorSubClass(TableValue tVal, List<DatabaseValue> dbVals) {
        println("private class "+tVal.javaClass+"Creator implements PreparedStatementCreator {");
        println("private "+tVal.javaClass+" "+tVal.javaClass.toLowerCase()+";");
        println("public "+tVal.javaClass+"Creator("+tVal.javaClass+" "+tVal.javaClass.toLowerCase()+") {");
        println("this."+tVal.javaClass.toLowerCase()+" = "+tVal.javaClass.toLowerCase()+";");
        println("}");
        println("@Override");
        println("public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {");
        println("PreparedStatement ps = connection.prepareStatement(");
        String insertStr = "      \"INSERT INTO "+tVal.databaseTable+" (";
        for(DatabaseValue dbVal : dbVals) {
            if(!dbVal.autoInc) {
                insertStr += dbVal.databaseCol+", ";
            }
        }
        insertStr = insertStr.substring(0, insertStr.length()-2)+") \"\n";
        insertStr += "      + \"VALUES (";
        for(DatabaseValue dbVal : dbVals) {
            if(!dbVal.autoInc) {
                if(dbVal.javaType.equals("Date")) {
                    insertStr += "now(), ";
                } else {
                    insertStr += "?, ";
                }
            }
        }
        insertStr = insertStr.substring(0, insertStr.length()-2)+") \", Statement.RETURN_GENERATED_KEYS);";
        println(insertStr);
        int counter = 1;
        for(DatabaseValue dbVal : dbVals) {
            if(!dbVal.autoInc && !dbVal.javaType.equals("Date")) {
                if(dbVal.javaType.equals("String")) {
                    println("ps.setString("+counter+", "+tVal.javaClass.toLowerCase()+".get"+dbVal.javaCapProp+"());");
                    counter++;
                } else if(dbVal.javaType.equals("int")){
                    println("ps.setInt("+counter+", "+tVal.javaClass.toLowerCase()+".get"+dbVal.javaCapProp+"());");
                    counter++;
                }
            }
        }
        println("return ps;");
        println("}");
        println("}");
    }

    void createDaoInterface(Map<TableValue, List<DatabaseValue>> tableMap) {
        println("package "+classPrefix.toLowerCase()+".dao;");
        println("");
        println("import java.util.List;");
        println("import "+classPrefix.toLowerCase()+".model.*;");
        println("");
        println("public interface "+classPrefix+"Dao {");
        for(TableValue tVal : tableMap.keySet()) {
            List<DatabaseValue> dbVals = tableMap.get(tVal);
            
            println("public List<"+tVal.javaClass+"> get"+tVal.javaClass+(tVal.javaClass.endsWith("s")?"":"s")+"();");
            println("public void create"+tVal.javaClass+"("+tVal.javaClass+" "+tVal.javaClass.toLowerCase()+");");
            println("public void update"+tVal.javaClass+"("+tVal.javaClass+" "+tVal.javaClass.toLowerCase()+");");
            for(DatabaseValue dbVal : dbVals) {
                if(dbVal.autoInc) {
                    println("public "+tVal.javaClass+" get"+tVal.javaClass+"ById(int "+dbVal.javaProp+");");
                }
            }
        }
        println("}");
        println("");
    }
    
    void generateModelClass(String capTable, List<DatabaseValue> dbValues) throws FileNotFoundException {
        String pkg = classPrefix.toLowerCase()+".model";
        String dirs = pkg.replaceAll("\\.", File.separator);
        
        setFileName(dirs, capTable+".java");
        println("package "+classPrefix.toLowerCase()+".model;");
        println("");
        println("import java.util.Date;");
        println("");
        println("public class "+capTable+" {");
        
        for(DatabaseValue dbValue : dbValues) {
            println("private "+dbValue.javaType+" "+dbValue.javaProp+";");
        }
        
        println("");
        
        for(DatabaseValue dbValue : dbValues) {
            println("public "+dbValue.javaType+" get"+dbValue.javaCapProp+"() {");
            println("return "+dbValue.javaProp+";");
            println("}");
            println("public void set"+dbValue.javaCapProp+"("+dbValue.javaType+" "+dbValue.javaProp+") {");
            println("this."+dbValue.javaProp+" = "+dbValue.javaProp+";");
            println("}");
        }
        println("}");
    }

    void incSpacing() {
        curSpacing += spacing;
    }
    void decSpacing() {
        curSpacing -= spacing;
    }
    public String setSpacing(String s, int spacing) {
        if(spacing==0) {
            return s;
        }
        String os = String.format("%"+spacing+"c",' ');
        return os+s;
    }
    
    public String println(String s) {
        if(s.startsWith("}")) {
            decSpacing();
        }
        s = setSpacing(s, curSpacing);
        pw.println(s);
        if(s.endsWith("{")) {
            incSpacing();
        }
        return s;
    }
    
    public void println() {
        pw.println();
    }
    
    public String convertSqlNameToProperty(String sqlName) {
        if(sqlName.indexOf("_")!=-1) {
            String propName = sqlName.substring(0,sqlName.indexOf("_"));
            String capName = sqlName.substring(sqlName.indexOf("_")+1);
            capName = capName.substring(0,1).toUpperCase()+capName.substring(1);
            
            return propName+convertSqlNameToProperty(capName);
        } else {
            return sqlName;
        }
    }
    /**
     * @param args Use it to specify an alternate config file, otherwise default will be too look for config.properties
     */
    public static void main(String[] args) throws SQLException, FileNotFoundException, IOException {
        
        GenerateClassesFromDatabase gc = new GenerateClassesFromDatabase();

        if(args.length>0) {
            Properties p = new Properties();
            p.load(new FileReader(args[0]));
            
            gc.configureFromProperties(p);  
        } else {
            File f = new File("config.properties");
            if(f.exists()) {
                Properties p = new Properties();
                p.load(new FileReader(f));

                gc.configureFromProperties(p);  
            }
        }
        gc.genereteClassesFromDatabase();
    }
    
}
