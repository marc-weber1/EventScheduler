<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<link rel="stylesheet" href="style.css">
</head>
<body><div class="presentation-sheet">

<?php

$_POST = array_map( 'htmlentities', $_POST );

function validateDate($date_string){
	$date_arr = explode("-",$date_string);
	if(count($date_arr)==3){
		if(checkdate($date_arr[1],$date_arr[2],$date_arr[0])){
			if(new DateTime($date_string) >= (new DateTime())->modify("-2 days")){
				return $date_string;
			}
		}
	}
	die('Invalid date: ' . $date_string);
}

function validateTime($time_string){
	//24-hour
	if( preg_match("/^(?:2[0-3]|[01][0-9]):[0-5][0-9]$/", $time_string) ){
		return $time_string;
	}
	die("Invalid time: " . $time_string);
}

//Requires all data is already valid, also everything but allow_rsvp needs to be a string
function saveToDataBase($eventname,$hostname,$date,$start_time,$end_time,
			$address,$city,$allow_rsvp,$details,$over_18){
	$serveraddr = "localhost";
	$dbname = "...";
	$sqlusername = "...";
	$sqlpassword = '...';

	$dsn = "mysql:host=$serveraddr;dbname=$dbname;charset=utf8mb4";
	$pdooptions = [
		PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
		PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
		PDO::ATTR_EMULATE_PREPARES   => false
	];
	try{
		$pdo = new PDO($dsn,$sqlusername,$sqlpassword,$pdooptions);
	} catch(\PDOException $e){
		die($e->getMessage());
	}

	$stmt = $pdo->prepare('INSERT INTO event (name,host,date,starttime,endtime,address,city,allowrsvp,details,over_18) VALUES (?,?,?,?,?,?,?,?,?,?)');
	$stmt->execute([$eventname,$hostname,$date,$start_time,$end_time,
			$address,$city,$allow_rsvp,$details,$over_18]);
	return $stmt->fetch();
}

//Make sure all the params exist
if( array_diff_key(array_flip( array('name','host','date','address','city') ), $_POST )){
	die("Missing required argument.");
}

//Get the non-required ones
$start_time = "";
if( array_key_exists("starttime",$_POST) ){
	if($_POST["starttime"] != "")
		$start_time = validateTime($_POST["starttime"]);
}
$end_time = "";
if( array_key_exists("endtime",$_POST) ){
	if($_POST["endtime"] != "")
		$end_time = validateTime($_POST["endtime"]);
}
$event_details = "";
if( array_key_exists("details",$_POST) ){
	$event_details = $_POST["details"];
}
$allow_rsvp = 0;
if( array_key_exists("rsvp",$_POST) ){
	$allow_rsvp = 1;
}
$over_18 = 0;
if( array_key_exists("over_18",$_POST) ){
$over_18 = 1;
}

saveToDataBase($_POST['name'],$_POST['host'],validateDate($_POST['date']),
		$start_time,$end_time,$_POST['address'],$_POST['city'],$allow_rsvp,
		$event_details,$over_18);

echo "Saved to database successfully.";


?>


<br/><br/>
<a href="./index.php">Back to events list</a>

</div></body></html>
