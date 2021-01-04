# jassd
Java Automated Spring Sourcecode for Databases generator.  

For those that:

1. Do not want to use an ORM like Hibernate and prefer to use simple JDBC
2. Use Spring Boot
3. Have an existing database that they need to create POJOs and Dao classes for

Then this project will automatically generate java classes representing the tables, Dao interfaces for talking to the database, Dao implementation classes for getting, updating, and inserting data, as well as a simple controller, that can be copied into a Spring Boot project and be used as a starting point for further development.  It eliminates all of the bootstrapping code that would be needed to setup simple database access.

Validated with MySQL databases so far.

## Usage
Download the jar file or build your own version, update the config.properties with the database connection information and default package that you want to use, and then execute the jar file to have sources generated.

## Example
Given a database with the following structure (a sql file with some sample data can be found [here](https://github.com/aforslund/jassd/blob/main/media/dbexample.sql?raw=true)):

![alt text](https://raw.githubusercontent.com/aforslund/jassd/main/media/dbexample.jpg "Database example")

And if the configuration file has the following:

```
database.server=127.0.0.1
database.port=3306
database.user=dbgenuser
database.password=12345
database.name=dbgentest
database.type=mysql
database.customConnectionString=?characterEncoding=utf8&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC

generator.basepackage=Jassd
generator.spacing=2
```

Then the following files will will be generated.  The generated files as a Spring Boot project (with only application.properties and Application.java added from what was generated) can be found [here](https://github.com/aforslund/jassd/blob/main/media/jassd_demo.zip?raw=true).

### Model files - POJOs with getter/setters representing tables in the database

* jassd.model.Car
* jassd.model.User
* jassd.model.UserCars

The Car class, for example, will then look like this:

```java
package jassd.model;

import java.util.Date;

public class Car {
  private int carId;
  private String make;
  private String model;
  private int year;
  private double price;
  private float rating;
  private String vin;
  
  public int getCarId() {
    return carId;
  }
  public void setCarId(int carId) {
    this.carId = carId;
  }
  public String getMake() {
    return make;
  }
  public void setMake(String make) {
    this.make = make;
  }
  public String getModel() {
    return model;
  }
  public void setModel(String model) {
    this.model = model;
  }
  public int getYear() {
    return year;
  }
  public void setYear(int year) {
    this.year = year;
  }
  public double getPrice() {
    return price;
  }
  public void setPrice(double price) {
    this.price = price;
  }
  public float getRating() {
    return rating;
  }
  public void setRating(float rating) {
    this.rating = rating;
  }
  public String getVin() {
    return vin;
  }
  public void setVin(String vin) {
    this.vin = vin;
  }
}
```

### Dao interfaces

* jassd.dao.JassdDao.java

It contains interfaces for all the models.  A nice future feature release would be to potentially seprate them to separate files, but it might also be nice to do that on your own as you work off of this basic set of files.

For example:

```java
public interface JassdDao {
  public List<Car> getCars();
  public void createCar(Car car);
  public void updateCar(Car car);
  public Car getCarById(int carId);
  /// Etc...
}
```

### Dao implementation classes

* jassd.dao.impl.JassdDaoImpl.java

Contains the actual jdbc sql commands, with both NamedParameterJdbcTemplate and JdbcTemplate implementation, using Updater, Creator, and Extractor subclasses.

For example:

```java
@Component("JassdDao")
public class JassdDaoImpl implements JassdDao {

  private JdbcTemplate jdbcTemplate;
  private NamedParameterJdbcTemplate namedJdbcTemplate;

  @Autowired
  private void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
    namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
  }

  @Override
  public List<Car> getCars() {
    return jdbcTemplate.query("SELECT * FROM car", new CarExtractor());
  }
  @Override
  public void createCar(Car car) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(new CarCreator(car), keyHolder);
    car.setCarId(keyHolder.getKey().intValue());
  }
  @Override
  public void updateCar(Car car) {
    jdbcTemplate.update(new CarUpdater(car));
  }
  @Override
  public Car getCarById(int carId) {
    Map namedParameters = Collections.singletonMap("carId", carId);
    return namedJdbcTemplate.queryForObject("SELECT * FROM car WHERE car_id=:carId", namedParameters, new CarExtractor());
  }
  private class CarCreator implements PreparedStatementCreator {
    private Car car;
    public CarCreator(Car car) {
      this.car = car;
    }
    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
      PreparedStatement ps = connection.prepareStatement(
            "INSERT INTO car (make, model, year, price, rating, vin) "
      + "VALUES (?, ?, ?, ?, ?, ?) ", Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, car.getMake());
      ps.setString(2, car.getModel());
      ps.setInt(3, car.getYear());
      ps.setString(4, car.getVin());
      return ps;
    }
  }
  // Etc...
}
```

### Basic controller

* jassd.controller.JassdController.java

And finally a basic controller that lets you access json REST calls.  Personally I prefer to put a service facade between the controller and Dao layer, but this is to give some basic setup to be able to get going.

```java
@RestController
@RequestMapping("/jassd")
public class JassdController {
  @Autowired
  private JassdDao jassdDao;
  @RequestMapping("cars")
  @ResponseBody
  public List<Car> getCars() {
    return jassdDao.getCars();
  }
  @RequestMapping("users")
  @ResponseBody
  public List<User> getUsers() {
    return jassdDao.getUsers();
  }
  @RequestMapping("usercars")
  @ResponseBody
  public List<UserCars> getUserCars() {
    return jassdDao.getUserCars();
  }
}
```

Running Application.java class and calling http://localhost:8080/jassd/cars would then result in the json response:

```
// 20210104220030
// http://localhost:8080/jassd/cars

[
  {
    "carId": 1,
    "make": "Ford",
    "model": "LTD",
    "year": 1980,
    "price": 19999.99,
    "rating": 4.5,
    "vin": "1234556"
  }
]
```


