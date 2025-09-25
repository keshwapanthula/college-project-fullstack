import PropTypes from "prop-types";
import { useState } from "react";
import { createMongoNotification, createsqlNotification } from "./NotificationService";

const CreateNotification = ({ refresh }) => {
  const [title, setTitle] = useState("");
  const [message, setMessage] = useState("");

  const handleCreate = async (isMongo = false) => {
    if (!title || !message) return;
    if (isMongo) await createMongoNotification({ title, message });
    else await createsqlNotification({ title, message });

    setTitle("");
    setMessage("");
    refresh(); // Refresh the list after creation
  };

  return (
    <div>
      <input placeholder="Title" value={title} onChange={e => setTitle(e.target.value)} />
      <input placeholder="Message" value={message} onChange={e => setMessage(e.target.value)} />
      <button onClick={() => handleCreate(false)}>Save to SQL</button>
      <button onClick={() => handleCreate(true)}>Save from Mongo</button>
    </div>
  ); 
};

CreateNotification.propTypes = {
  refresh: PropTypes.func.isRequired,
};

export default CreateNotification;
