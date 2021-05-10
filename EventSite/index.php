<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<link rel="stylesheet" href="style.css">
</head>

<body><div class="presentation-sheet">

    <p>CANADA'S COVID ALERT SYSTEM HAS BEEN RELEASED! <br/>
      <a href="https://play.google.com/store/apps/details?id=ca.gc.hcsc.canada.stopcovid">Android</a> | <a href="https://apps.apple.com/ca/app/covid-alert/id1520284227">iOS</a><br/>
      Please install and enable the app before going to any kind of meetup. Note that although it is not yet functional in Alberta, turning it on will still help you even if reporting doesn't open for 14 more days.
    </p><br/>
    
<a href="./neweventform.html">Create a new event</a><br/><br/>

<?php

function create_links($text){
	$reg_exUrl = "/(http|https|ftp|ftps)\:\/\/[a-zA-Z0-9\-\.]+\.[a-zA-Z]{2,3}(\/\S*)?/";

	return preg_replace($reg_exUrl,"<a href=\"$0\">$0</a>",$text);
}

$serveraddr = "localhost";
$sqlusername="...";
$sqlpassword='...';
$dbname="...";

$dsn = "mysql:host=$serveraddr;dbname=$dbname;charset=utf8mb4";
$options = [
	PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
	PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
	PDO::ATTR_EMULATE_PREPARES   => false
];
$pdo = new PDO($dsn,$sqlusername,$sqlpassword,$options);

if( array_key_exists("city",$_GET) ){
	$stmt = $pdo->prepare('SELECT * FROM event WHERE city=? AND DATEDIFF( date, CURDATE() ) > -1 ORDER BY date ASC');
	$stmt->execute([$_GET["city"]]);
}
else{
	$stmt = $pdo->query('SELECT * FROM event WHERE DATEDIFF( date, CURDATE() ) >= 0 ORDER BY date ASC');
}

while($row = $stmt->fetch()){
		echo "<h2>" . $row['name'] . "</h2>";
		echo "<h4>Hosted by " . $row['host'] . "</h4>";
		echo "<h4>Where: " . $row['address'] . ", " . $row['city'] . "</h4>";
		echo "<h4>When: " . $row['date'] . " from " .
			(new \DateTime($row['starttime']))->format('H:i')
			. " to " .
			(new \DateTime($row['endtime']))->format('H:i')
			. "</h4>";
		if($row['over_18']==1){
			echo "<h5>18+</h5>";
		}

		$description = str_replace("\n","<br/>",$row['details']);

		echo create_links($description);
		echo "<br/><br/><br/><br/>";
}


?>

<a href="./neweventform.html">Create a new event</a>

</div></body></html>
