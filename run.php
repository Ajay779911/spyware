<?php
// save.php on your server
$data = file_get_contents('php://input');
file_put_contents('collected_data.txt', $data . "\n", FILE_APPEND);
?>