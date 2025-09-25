import CreateNotification from "./CreateNotification";
import Notifications from "./Notifications";

function App() {
  return (
    <div className="App">
      <h1>College Notifications</h1>
      <CreateNotification refresh={() => window.location.reload()} />
      <Notifications />
    </div>
  );
}

export default App;
