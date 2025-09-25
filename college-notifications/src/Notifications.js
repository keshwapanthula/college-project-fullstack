import { useEffect, useState } from "react";
import { getAllMongoNotifications, getAllNotifications, markAsRead, markMongoAsRead } from "./NotificationService";

const Notifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [mongoNotifications, setMongoNotifications] = useState([]);

  const fetchNotifications = async () => {
    const sqlRes = await getAllNotifications();
    setNotifications(sqlRes.data);

    const mongoRes = await getAllMongoNotifications();
    setMongoNotifications(mongoRes.data);
  };

  const handleMarkRead = async (id, isMongo = false) => {
    if (isMongo) await markMongoAsRead(id);
    else await markAsRead(id);
    fetchNotifications();
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  return (
    <div style={{ padding: "2rem" }}>
      <h2>SQL Notifications</h2>
      <ul>
        {notifications.map(n => (
          <li key={n.id}>
            <strong>{n.title}</strong>: {n.message} 
            <button onClick={() => handleMarkRead(n.id)}>Mark as Read</button>
          </li>
        ))}
      </ul>

      <h2>Mongo Notifications</h2>
      <ul>
        {mongoNotifications.map(n => (
          <li key={n.id}>
            <strong>{n.title}</strong>: {n.message} 
            <button onClick={() => handleMarkRead(n.id, true)}>Mark as Read</button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default Notifications;
