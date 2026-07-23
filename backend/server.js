const express = require('express');
const app = express();
const PORT = 3000;

app.get('*', (req, res) => {
  res.send(`
    <html>
      <body style="font-family: system-ui, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; background-color: #f8fafc; color: #0f172a;">
        <div style="text-align: center; padding: 3rem; background: white; border-radius: 16px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); max-width: 500px;">
          <h2 style="margin-top: 0;">📱 Android Project Ready</h2>
          <p style="line-height: 1.5; color: #475569;">
            This workspace has been converted into a native Android Studio project using <strong>Kotlin</strong>, <strong>Jetpack Compose</strong>, and <strong>Room Database</strong>.
          </p>
          <p style="line-height: 1.5; color: #475569;">
            Native Android applications cannot run directly inside this web browser preview.
          </p>
          <div style="background: #eff6ff; padding: 1.5rem; border-radius: 8px; margin-top: 1.5rem; text-align: left;">
            <p style="margin-top: 0; font-weight: 600; color: #1e3a8a;">To view and run your app:</p>
            <ol style="margin: 0; padding-left: 1.5rem; color: #1e40af; line-height: 1.6;">
              <li>Click the <strong>Settings / Export</strong> menu in the top right.</li>
              <li>Select <strong>Download as ZIP</strong> or Export to GitHub.</li>
              <li>Extract the ZIP and open the folder in <strong>Android Studio</strong>.</li>
            </ol>
          </div>
        </div>
      </body>
    </html>
  `);
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(\`Placeholder server running on port \${PORT}\`);
});
