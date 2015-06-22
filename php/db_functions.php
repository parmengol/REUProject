<?php
 
class DB_Functions {
 
    private $db;
 
    //put your code here
    // constructor
    function __construct() {
        include_once './db_connect.php';
        // connecting to database
        $this->db = new DB_Connect();
        $this->db->connect();
    }
 
    // destructor
    function __destruct() {
 
    }
 
    /**
     * Storing new user
     * returns user details
     */
    public function storeReading($datetime,$mapx,$mapy,$rss,$ap_name,$mac,$map) {
        // Insert user into database
        //$result = mysql_query("INSERT INTO testtable VALUES($datetime,$mapx,$mapy,$rss,$ap_name,$mac,$map)");
        
        $result = mysql_query("INSERT INTO testtable VALUES(NULL,$datetime,$mapx,$mapy,$rss,'$ap_name','$mac',$map)");
        if ($result) {
            return true;
        } else {
            if( mysql_errno() == 1062) {
                // Duplicate key - Primary Key Violation
                return true;
            } else {
                // For other errors
                return false;
            }            
        }
    }
     /**
     * Getting all users
     */
    public function getAllReadings() {
        $result = mysql_query("select * FROM users");
        return $result;
    }
}
 
?>
