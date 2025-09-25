import { useState } from "react";

function CollegePortal() {
  const [updates, setUpdates] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [admins, setAdmins] = useState([]);

  const fetchUpdates = async () => {
    const res = await fetch("http://localhost:8080/collegeupdates");
    const data = await res.json();
    setUpdates(data);
  };

  const fetchNotifications = async () => {
    const res = await fetch("http://localhost:8083/notifications");
    const data = await res.json();
    setNotifications(data);
  };

  const fetchAdmins = async () => {
    const res = await fetch("http://localhost:8082/admin");
    const data = await res.json();
    setAdmins(data);
  };

  return (
    <div>
      <h1>College Portal</h1>

      <button onClick={fetchUpdates}>Updates</button>
      <ul>
        {updates.map(u => <li key={u.id}>{u.title} - {u.description}</li>)}
      </ul>

      <button onClick={fetchNotifications}>Notifications</button>
      <ul>
        {notifications.map(n => <li key={n.id}>{n.message}</li>)}
      </ul>

      <button onClick={fetchAdmins}>Admins</button>
      <ul>
        {admins.map(a => <li key={a.id}>{a.name} - {a.role}</li>)}
      </ul>
    </div>
  );
}

export default CollegePortal;

