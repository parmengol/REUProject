<?php
/**
 * Creates Unsynced rows data as JSON
 */
    include_once 'db_functions.php';
    $db = new DB_Functions();
    $metapoints = $db->getMetaDataPoints();
    //$metaaps = $db->getMetaDataAPs();
    $a = array();
    $b = array();
    //$c = array();
    if ($metapoints != false){
        while ($row = mysql_fetch_array($metapoints)) { 
            $b["mapx"] = $row["mapx"];
            $b["mapy"] = $row["mapy"];
            array_push($a,$b);
        }
      echo json_encode($a);
    }
//    if ($metaaps != false) {
//        while ($row = mysql_fetch_array($metaaps)) {
//            $c["mac"] = $row["mac"];
//            array_push($a,$c);
//        }
//        echo json_encode($a);
//    }
?>
