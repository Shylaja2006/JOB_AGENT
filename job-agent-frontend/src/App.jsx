import { useEffect, useEffectEvent, useState } from "react";
import "./App.css";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const APPLICATION_STATUSES = [
  "SAVED",
  "APPLIED",
  "INTERVIEW",
  "REJECTED",
  "OFFER",
];

const renderSkills = (skills) => {
  if (Array.isArray(skills)) {
    return skills.filter(Boolean);
  }

  if (typeof skills === "string" && skills.trim()) {
    return skills
      .split(/[,;|•\n]/)
      .map((skill) => skill.trim())
      .filter(Boolean);
  }

  return [];
};

const getScoreTier = (score) => {
  const numericScore = Number(score);
  if (numericScore >= 90) return "excellent";
  if (numericScore >= 75) return "strong";
  if (numericScore >= 60) return "moderate";
  return "developing";
};

const getRouteFromHash = () => {
  const route = window.location.hash.replace(/^#\/?/, "").split("?")[0];
  return route || "dashboard";
};

function App() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showRegister, setShowRegister] = useState(false);
  const [registerName, setRegisterName] = useState("");
  const [registerEmail, setRegisterEmail] = useState("");
  const [registerPassword, setRegisterPassword] = useState("");
  const [registerConfirmPassword, setRegisterConfirmPassword] = useState("");
  const [registerLoading, setRegisterLoading] = useState(false);
  const [registerMessage, setRegisterMessage] = useState("");

  const [token, setToken] = useState(
    localStorage.getItem("jobAgentToken") || ""
  );

  const [jobs, setJobs] = useState([]);
  const [location, setLocation] = useState("Hyderabad");
  const [role, setRole] = useState("");
  const [skills, setSkills] = useState("");
  const [minScore, setMinScore] = useState(0);
  const [resumeFile, setResumeFile] = useState(null);
  const [resume, setResume] = useState(null);
  const [resumeName, setResumeName] = useState("");
  const [resumeLoading, setResumeLoading] = useState(false);
  const [resumeMessage, setResumeMessage] = useState("");
  const [applications, setApplications] = useState([]);
  const [applicationsLoading, setApplicationsLoading] = useState(false);
  const [applicationMessage, setApplicationMessage] = useState("");
  const [applicationFilter, setApplicationFilter] = useState(() =>
    getRouteFromHash() === "saved-jobs" ? "SAVED" : "ALL"
  );
  const [savingJobKey, setSavingJobKey] = useState("");
  const [updatingApplicationId, setUpdatingApplicationId] = useState(null);
  const [deletingApplicationId, setDeletingApplicationId] = useState(null);
  const [profile, setProfile] = useState(null);

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [loggedIn, setLoggedIn] = useState(!!token);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeRoute, setActiveRoute] = useState(getRouteFromHash);

  useEffect(() => {
    const handleHashChange = () => {
      const nextRoute = getRouteFromHash();
      setActiveRoute(nextRoute);
      setApplicationFilter(nextRoute === "saved-jobs" ? "SAVED" : "ALL");
      setSidebarOpen(false);
    };

    window.addEventListener("hashchange", handleHashChange);
    return () => window.removeEventListener("hashchange", handleHashChange);
  }, []);

  useEffect(() => {
    const routeTargets = {
      dashboard: "dashboard",
      "find-jobs": "find-jobs",
      "saved-jobs": "applications",
      applications: "applications",
      resume: "resume",
      profile: "profile",
      "career-tips": "ai-insights",
    };
    const targetId = routeTargets[activeRoute] || "dashboard";

    requestAnimationFrame(() => {
      document.getElementById(targetId)?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    });
  }, [activeRoute]);

  const navigateTo = (event, route) => {
    event.preventDefault();
    const nextHash = `#/${route}`;
    if (window.location.hash === nextHash) {
      setActiveRoute(route);
      setApplicationFilter(route === "saved-jobs" ? "SAVED" : "ALL");
      return;
    }
    window.location.hash = nextHash;
  };

  // =========================
  // LOGIN
  // =========================
  const login = async (e) => {
    e.preventDefault();

    try {
      setMessage("Logging in...");

      const response = await fetch(`${API_URL}/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          password,
        }),
      });

      if (!response.ok) {
        throw new Error("Invalid email or password");
      }

      const jwt = await response.text();

      localStorage.setItem("jobAgentToken", jwt);
      setToken(jwt);
      setLoggedIn(true);
      setMessage("Login successful!");

    } catch (error) {
      setMessage(error.message);
    }
  };

  const register = async (e) => {
    e.preventDefault();

    if (registerPassword !== registerConfirmPassword) {
      setRegisterMessage("Passwords do not match.");
      return;
    }

    try {
      setRegisterLoading(true);
      setRegisterMessage("");

      const response = await fetch(`${API_URL}/auth/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          name: registerName,
          email: registerEmail,
          password: registerPassword,
        }),
      });

      if (!response.ok) {
        let errorMessage = "Unable to create your account. Please try again.";
        const contentType = response.headers.get("content-type") || "";
        if (contentType.includes("application/json")) {
          const errorData = await response.json();
          errorMessage = errorData.message || errorData.error || errorMessage;
        } else {
          const text = (await response.text()).trim();
          if (text) {
            errorMessage = text;
          }
        }
        throw new Error(errorMessage);
      }

      setShowRegister(false);
      setEmail(registerEmail);
      setPassword("");
      setRegisterName("");
      setRegisterEmail("");
      setRegisterPassword("");
      setRegisterConfirmPassword("");
      setMessage("Account created successfully. Please sign in.");
    } catch (error) {
      setRegisterMessage(
        error.message || "Unable to create your account. Please try again."
      );
    } finally {
      setRegisterLoading(false);
    }
  };

  // =========================
  // GET RECOMMENDED JOBS
  // =========================
  const getRecommendedJobs = async () => {
    if (!token) {
      setMessage("Please login first.");
      return;
    }

    try {
      setLoading(true);
      setMessage("");

      const params = new URLSearchParams();
      params.set("location", location.trim() || "Hyderabad");
      if (role.trim()) {
        params.set("role", role.trim());
      }
      if (skills.trim()) {
        params.set("skills", skills.trim());
      }
      if (minScore > 0) {
        params.set("minScore", String(minScore));
      }

      const response = await fetch(
        `${API_URL}/api/jobs/recommended?${params.toString()}`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.status === 401 || response.status === 403) {
        throw new Error("Session expired. Please login again.");
      }

      if (response.status === 400) {
        let validationMessage = "Please check your search filters and try again.";
        try {
          const errorData = await response.json();
          validationMessage =
            errorData.message || errorData.error || validationMessage;
        } catch {
          // Keep the friendly fallback when the backend has no JSON error body.
        }
        throw new Error(validationMessage);
      }

      if (!response.ok) {
        throw new Error("Unable to load jobs right now. Please try again.");
      }

      const data = await response.json();

      setJobs(data);

      if (data.length === 0) {
        setMessage(
          "No jobs found matching your filters. Try changing the role, skills, location, or minimum score."
        );
      } else {
        setMessage(`${data.length} recommended jobs found.`);
      }

    } catch (error) {
      setMessage(
        error.message || "Unable to load jobs right now. Please try again."
      );

      if (error.message.includes("Session expired")) {
        logout("Session expired. Please login again.");
      }
    } finally {
      setLoading(false);
    }
  };

  const clearFilters = () => {
    setLocation("Hyderabad");
    setRole("");
    setSkills("");
    setMinScore(0);
    setJobs([]);
    setMessage("");
  };

  const loadApplications = async () => {
    if (!token) {
      return;
    }

    try {
      setApplicationsLoading(true);
      const response = await fetch(`${API_URL}/api/applications`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.status === 401 || response.status === 403) {
        throw new Error("Session expired. Please login again.");
      }

      if (!response.ok) {
        throw new Error("Unable to load applications right now.");
      }

      setApplications(await response.json());
    } catch (error) {
      setApplicationMessage(
        error.message.includes("Session expired")
          ? error.message
          : "Unable to load applications right now."
      );
      if (error.message.includes("Session expired")) {
        logout("Session expired. Please login again.");
      }
    } finally {
      setApplicationsLoading(false);
    }
  };

  const saveJob = async (job) => {
    if (!token) {
      setMessage("Please login first.");
      return;
    }

    const jobKey = job.url || `${job.title}|${job.companyName}`;
    if (savingJobKey === jobKey || findSavedApplication(job)) {
      return;
    }

    try {
      setSavingJobKey(jobKey);
      setApplicationMessage("");
      const response = await fetch(`${API_URL}/api/applications`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          title: job.title || "Job Opportunity",
          company: job.companyName || "Company not specified",
          location: job.location || "",
          jobUrl: job.url || "",
          status: "SAVED",
        }),
      });

      if (response.status === 401 || response.status === 403) {
        throw new Error("Session expired. Please login again.");
      }

      if (!response.ok) {
        throw new Error("Unable to save this job right now.");
      }

      const savedApplication = await response.json();
      setApplications((current) => [...current, savedApplication]);
      setApplicationMessage("Job saved to your applications.");
    } catch (error) {
      setApplicationMessage(
        error.message.includes("Session expired")
          ? error.message
          : "Unable to save this job right now."
      );
      if (error.message.includes("Session expired")) {
        logout("Session expired. Please login again.");
      }
    } finally {
      setSavingJobKey("");
    }
  };

  const updateApplicationStatus = async (application, status) => {
    try {
      setUpdatingApplicationId(application.id);
      setApplicationMessage("");
      const response = await fetch(
        `${API_URL}/api/applications/${application.id}/status`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ status }),
        }
      );

      if (response.status === 401 || response.status === 403) {
        throw new Error("Session expired. Please login again.");
      }

      if (!response.ok) {
        throw new Error("Unable to update application status right now.");
      }

      const updatedApplication = await response.json();
      setApplications((current) =>
        current.map((item) =>
          item.id === updatedApplication.id ? updatedApplication : item
        )
      );
    } catch (error) {
      setApplicationMessage(
        error.message.includes("Session expired")
          ? error.message
          : "Unable to update application status right now."
      );
      if (error.message.includes("Session expired")) {
        logout("Session expired. Please login again.");
      }
    } finally {
      setUpdatingApplicationId(null);
    }
  };

  const deleteApplication = async (applicationId) => {
    try {
      setDeletingApplicationId(applicationId);
      setApplicationMessage("");
      const response = await fetch(
        `${API_URL}/api/applications/${applicationId}`,
        {
          method: "DELETE",
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (response.status === 401 || response.status === 403) {
        throw new Error("Session expired. Please login again.");
      }

      if (!response.ok) {
        throw new Error("Unable to delete this application right now.");
      }

      setApplications((current) =>
        current.filter((application) => application.id !== applicationId)
      );
    } catch (error) {
      setApplicationMessage(
        error.message.includes("Session expired")
          ? error.message
          : "Unable to delete this application right now."
      );
      if (error.message.includes("Session expired")) {
        logout("Session expired. Please login again.");
      }
    } finally {
      setDeletingApplicationId(null);
    }
  };

  const findSavedApplication = (job) =>
    applications.find(
      (application) =>
        (job.url && application.jobUrl === job.url) ||
        (!job.url &&
          application.title === job.title &&
          application.company === job.companyName)
    );

  const getApplicationStatus = (application) =>
    application.status === "OFFERED" ? "OFFER" : application.status;

  const filteredApplications =
    applicationFilter === "ALL"
      ? applications
      : applications.filter(
          (application) =>
            getApplicationStatus(application) === applicationFilter
        );

  const formatApplicationDate = (application) => {
    const dateValue =
      application.appliedAt ||
      application.savedAt ||
      application.createdAt ||
      application.updatedAt;

    if (!dateValue) {
      return null;
    }

    const date = new Date(dateValue);
    return Number.isNaN(date.getTime())
      ? String(dateValue)
      : date.toLocaleDateString();
  };

  const applicationCounts = APPLICATION_STATUSES.reduce(
    (counts, status) => {
      counts[status] = applications.filter(
        (application) =>
          getApplicationStatus(application) === status
      ).length;
      return counts;
    },
    {}
  );

  const averageMatchScore = jobs.length > 0
    ? Math.round(
        jobs.reduce((total, job) => total + Number(job.score || 0), 0) /
          jobs.length
      )
    : null;

  const hasAdditionalFilters =
    role.trim() || skills.trim() || minScore > 0;
  const profileName = profile?.name || "";
  const readinessChecks = [
    { label: "Profile completed", complete: Boolean(profile?.name && email) },
    { label: "Resume uploaded", complete: Boolean(resume) },
    { label: "Skills added", complete: renderSkills(resume?.skills).length > 0 },
    { label: "Applications started", complete: applications.length > 0 },
  ];
  const readinessScore = Math.round(
    (readinessChecks.filter((item) => item.complete).length / readinessChecks.length) * 100
  );
  const recentApplications = [...applications]
    .sort((first, second) => {
      const firstDate = new Date(first.updatedAt || first.createdAt || 0).getTime();
      const secondDate = new Date(second.updatedAt || second.createdAt || 0).getTime();
      return secondDate - firstDate;
    })
    .slice(0, 3);

  const loadResumeProfile = async () => {
    if (!token) {
      return;
    }

    try {
      setResumeLoading(true);
      setResumeMessage("");

      const response = await fetch(`${API_URL}/api/resume`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.status === 401 || response.status === 403) {
        throw new Error("Session expired. Please login again.");
      }

      if (response.status === 404) {
        setResume(null);
        setResumeName("");
        setResumeMessage("No resume uploaded yet.");
        return;
      }

      if (!response.ok) {
        throw new Error("Failed to load resume profile");
      }

      const data = await response.json();
      setResume(data);
      setResumeName(data.title || "");
    } catch (error) {
      setResumeMessage(error.message);

      if (error.message.includes("Session expired")) {
        logout("Session expired. Please login again.");
      }
    } finally {
      setResumeLoading(false);
    }
  };

  const loadUserProfile = async () => {
    if (!token) {
      return;
    }

    const response = await fetch(`${API_URL}/api/profile`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (response.status === 401 || response.status === 403) {
      throw new Error("Session expired. Please login again.");
    }

    if (!response.ok) {
      throw new Error("Unable to load profile.");
    }

    setProfile(await response.json());
  };

  // =========================
  // UPLOAD RESUME
  // =========================
  const uploadResume = async () => {
    if (!token) {
      setResumeMessage("Please login first.");
      return;
    }

    if (!resumeFile) {
      setResumeMessage("Please select a PDF or DOC/DOCX file first.");
      return;
    }

    try {
      setResumeLoading(true);
      setResumeMessage("");

      const formData = new FormData();
      formData.append("file", resumeFile);

      const response = await fetch(`${API_URL}/api/resume/upload`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      if (response.status === 401 || response.status === 403) {
        throw new Error("Session expired. Please login again.");
      }

      if (!response.ok) {
        throw new Error("Failed to upload resume");
      }

      const data = await response.json();
      const resumeData = data.resume || data;
      setResume(resumeData);
      setResumeName(data.fileName || resumeData.title || "");
      setResumeMessage("Resume uploaded successfully.");
    } catch (error) {
      setResumeMessage(error.message);

      if (error.message.includes("Session expired")) {
        logout("Session expired. Please login again.");
      }
    } finally {
      setResumeLoading(false);
    }
  };

  // =========================
  // LOGOUT
  // =========================
  const logout = (reason = "Logged out successfully.") => {
    localStorage.removeItem("jobAgentToken");
    setToken("");
    setLoggedIn(false);
    setJobs([]);
    setResumeFile(null);
    setResume(null);
    setResumeName("");
    setResumeMessage("");
    setProfile(null);
    setApplications([]);
    setApplicationMessage("");
    setMessage(reason);
  };

  // Automatically load recommendations after login
  const loadDashboard = useEffectEvent(() => {
    if (token) {
      getRecommendedJobs();
      loadResumeProfile();
      loadApplications();
      loadUserProfile().catch((error) => {
        if (error.message.includes("Session expired")) {
          logout("Session expired. Please login again.");
        }
      });
    }
  });

  useEffect(() => {
    if (token) {
      queueMicrotask(loadDashboard);
    }
  }, [token]);

  // =========================
  // LOGIN PAGE
  // =========================
  if (!loggedIn) {
    return (
      <div className="app">
        <div className="login-container">
          <div className="login-card">
            <div className="auth-brand">
              <span className="brand-mark">✦</span>
              <div>
                <h1>Job Agent</h1>
                <p className="subtitle">AI-powered job recommendations</p>
              </div>
            </div>

            {showRegister ? (
              <form className="auth-form" onSubmit={register}>
                <label>Full Name</label>
                <input
                  type="text"
                  value={registerName}
                  onChange={(e) => setRegisterName(e.target.value)}
                  placeholder="Enter your full name"
                  required
                />

                <label>Email</label>
                <input
                  type="email"
                  value={registerEmail}
                  onChange={(e) => setRegisterEmail(e.target.value)}
                  placeholder="Enter your email"
                  required
                />

                <label>Password</label>
                <input
                  type="password"
                  value={registerPassword}
                  onChange={(e) => setRegisterPassword(e.target.value)}
                  placeholder="Create a password"
                  required
                />

                <label>Confirm Password</label>
                <input
                  type="password"
                  value={registerConfirmPassword}
                  onChange={(e) => setRegisterConfirmPassword(e.target.value)}
                  placeholder="Confirm your password"
                  required
                />

                <button type="submit" disabled={registerLoading}>
                  {registerLoading ? "Creating Account..." : "Create Account"}
                </button>

                {registerMessage && (
                  <p className="message error-message">{registerMessage}</p>
                )}

                <button
                  type="button"
                  className="auth-switch-button"
                  onClick={() => {
                    setShowRegister(false);
                    setRegisterMessage("");
                  }}
                >
                  Already have an account? Sign In
                </button>
              </form>
            ) : (
              <>
                <form className="auth-form" onSubmit={login}>
                  <label>Email</label>

                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="Enter your email"
                    required
                  />

                  <label>Password</label>

                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter your password"
                    required
                  />

                  <button type="submit">
                    Login
                  </button>
                </form>

                {message && (
                  <p className="message">
                    {message}
                  </p>
                )}

                <button
                  type="button"
                  className="auth-switch-button"
                  onClick={() => {
                    setShowRegister(true);
                    setMessage("");
                    setRegisterMessage("");
                  }}
                >
                  Don't have an account? Create Account
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    );
  }

  // =========================
  // DASHBOARD
  // =========================
  return (
    <div className="app">
      <div className={`dashboard-shell ${sidebarOpen ? "sidebar-visible" : ""}`}>
        <aside className="sidebar">
          <div className="sidebar-label">Workspace</div>
          <nav>
            <a className={`sidebar-link ${activeRoute === "dashboard" ? "active" : ""}`} href="#/dashboard" onClick={(event) => navigateTo(event, "dashboard")}>⌂ <span>Dashboard</span></a>
            <a className={`sidebar-link ${activeRoute === "find-jobs" ? "active" : ""}`} href="#/find-jobs" onClick={(event) => navigateTo(event, "find-jobs")}>⌕ <span>Find Jobs</span></a>
            <a className={`sidebar-link ${activeRoute === "saved-jobs" ? "active" : ""}`} href="#/saved-jobs" onClick={(event) => navigateTo(event, "saved-jobs")}>♡ <span>Saved Jobs</span></a>
            <a className={`sidebar-link ${activeRoute === "applications" ? "active" : ""}`} href="#/applications" onClick={(event) => navigateTo(event, "applications")}>▤ <span>Applications</span></a>
            <a className={`sidebar-link ${activeRoute === "resume" ? "active" : ""}`} href="#/resume" onClick={(event) => navigateTo(event, "resume")}>▣ <span>Resume</span></a>
            <a className={`sidebar-link ${activeRoute === "profile" ? "active" : ""}`} href="#/profile" onClick={(event) => navigateTo(event, "profile")}>◉ <span>My Profile</span></a>
            <a className={`sidebar-link ${activeRoute === "career-tips" ? "active" : ""}`} href="#/career-tips" onClick={(event) => navigateTo(event, "career-tips")}>✦ <span>AI Career Tips</span></a>
          </nav>
          <button type="button" className="sidebar-logout" onClick={logout}>↪ <span>Logout</span></button>
        </aside>

        <div className="dashboard-main">
          <header className="navbar">
            <div className="brand-lockup">
              <span className="brand-mark">✦</span>
              <div>
                <h1>Job Agent</h1>
                <p>AI-powered job recommendations</p>
              </div>
            </div>
            <div className="navbar-profile">
              <span className="navbar-avatar">{profileName ? profileName.charAt(0).toUpperCase() : "👤"}</span>
              <div className="navbar-user">
                <strong>{profileName || email || "Job seeker"}</strong>
                <span>{email || "Signed in"}</span>
              </div>
              <button className="logout-btn" onClick={logout}>Logout</button>
            </div>
          </header>

          <button
            type="button"
            className="mobile-menu-button"
            onClick={() => setSidebarOpen((open) => !open)}
            aria-expanded={sidebarOpen}
          >
            <span>☰</span> Menu
          </button>

          <main className="dashboard" id="dashboard">

        <section className="search-section" id="find-jobs">

          <p className="eyebrow">PERSONALIZED FOR YOUR NEXT CAREER MOVE</p>
          <h2>Find Your Next Opportunity</h2>
          <p className="search-description">
            Discover jobs matched to your skills, experience and preferred location.
          </p>

          <div className="hero-actions">
            <div className="hero-copy">
              <strong>Good {new Date().getHours() < 12 ? "morning" : new Date().getHours() < 18 ? "afternoon" : "evening"}, {profileName || "career seeker"} 👋</strong>
              <span>Let&apos;s find the best opportunities for your next career move.</span>
            </div>
            <div className="hero-action-buttons">
              <a className="hero-primary-action" href="#/find-jobs" onClick={(event) => navigateTo(event, "find-jobs")}>⌕ Find Jobs</a>
              <a className="hero-secondary-action" href="#/resume" onClick={(event) => navigateTo(event, "resume")}>↑ Upload Resume</a>
            </div>
          </div>

          <div className="search-filters">
            <label>
              <span>Location</span>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="Enter location"
              />
            </label>

            <label>
              <span>Job Role</span>
              <input
                type="text"
                value={role}
                onChange={(e) => setRole(e.target.value)}
                placeholder="e.g. React Developer"
              />
            </label>

            <label>
              <span>Skills</span>
              <input
                type="text"
                value={skills}
                onChange={(e) => setSkills(e.target.value)}
                placeholder="e.g. React, JavaScript, Node.js"
              />
            </label>

            <label className="score-filter">
              <span>Minimum Match Score: {minScore}</span>
              <input
                type="range"
                min="0"
                max="100"
                value={minScore}
                onChange={(e) => setMinScore(Number(e.target.value))}
              />
            </label>
          </div>

          <div className="search-actions">
            <button
              onClick={getRecommendedJobs}
              disabled={loading}
            >
              {loading ? "Searching..." : "Find Jobs"}
            </button>
            <button
              className="clear-filters-btn"
              onClick={clearFilters}
              disabled={loading}
            >
              Clear Filters
            </button>
          </div>

          {message && (
            <p className="message">
              {message}
            </p>
          )}

        </section>

        <section className="quick-stats" aria-label="Job search statistics">
          <div className="quick-stat quick-stat-purple">
            <span className="quick-stat-icon">✦</span>
            <div><strong>{jobs.length}</strong><span>Recommended Jobs</span></div>
          </div>
          <div className="quick-stat quick-stat-blue">
            <span className="quick-stat-icon">♡</span>
            <div><strong>{applicationCounts.SAVED}</strong><span>Saved Jobs</span></div>
          </div>
          <div className="quick-stat quick-stat-cyan">
            <span className="quick-stat-icon">↗</span>
            <div><strong>{applicationCounts.APPLIED}</strong><span>Applied Jobs</span></div>
          </div>
          <div className="quick-stat quick-stat-orange">
            <span className="quick-stat-icon">◷</span>
            <div><strong>{applicationCounts.INTERVIEW}</strong><span>Interview Scheduled</span></div>
          </div>
          <div className="quick-stat quick-stat-green">
            <span className="quick-stat-icon">★</span>
            <div><strong>{applicationCounts.OFFER}</strong><span>Offers</span></div>
          </div>
        </section>

        <div className="dashboard-insights">
        <section className="profile-section" id="profile">
          <div className="profile-heading">
            <div>
              <p className="eyebrow">ACCOUNT</p>
              <h2>My Profile</h2>
              <p className="search-description">
                Your Job Agent account and application snapshot.
              </p>
            </div>
            <span className="profile-avatar" aria-hidden="true">
              {profileName ? profileName.charAt(0).toUpperCase() : "👤"}
            </span>
          </div>

          <div className="profile-layout">
            <div className="profile-identity">
              <div className="profile-field">
                <span>Full Name</span>
                <strong>{profileName || "Not available"}</strong>
              </div>
              <div className="profile-field">
                <span>Email</span>
                <strong>{email || "Not available"}</strong>
              </div>
              <div className="profile-field">
                <span>Account Status</span>
                <strong className="profile-status">Signed in</strong>
              </div>
              <div className="profile-field">
                <span>Resume Status</span>
                <strong className={resume ? "profile-status" : "profile-muted"}>
                  {resumeLoading
                    ? "Loading..."
                    : resume
                      ? "Resume Uploaded"
                      : "No Resume Uploaded"}
                </strong>
              </div>
            </div>

            <div className="profile-stat-grid">
              <div className="profile-stat">
                <strong>{applicationCounts.SAVED}</strong>
                <span>Saved Applications</span>
              </div>
              <div className="profile-stat">
                <strong>{applicationCounts.APPLIED}</strong>
                <span>Applied Applications</span>
              </div>
              <div className="profile-stat">
                <strong>{applicationCounts.INTERVIEW}</strong>
                <span>Interview Applications</span>
              </div>
              <div className="profile-stat">
                <strong>{applicationCounts.OFFER}</strong>
                <span>Offers</span>
              </div>
            </div>
          </div>

          <button type="button" className="profile-logout-btn" onClick={logout}>
            Logout
          </button>
        </section>

        <section className="overview-section">
          <div className="section-header overview-header">
            <div>
              <p className="eyebrow">AT A GLANCE</p>
              <h2>Dashboard Overview</h2>
            </div>
            <span className="overview-updated">Live from your workspace</span>
          </div>

          <div className="overview-grid">
            <div className="overview-card overview-card-primary">
              <span>Total Recommended Jobs</span>
              <strong>{jobs.length}</strong>
              <small>Current search results</small>
            </div>
            <div className="overview-card">
              <span>Total Applications</span>
              <strong>{applications.length}</strong>
              <small>Tracked opportunities</small>
            </div>
            {APPLICATION_STATUSES.map((status) => (
              <div className="overview-card" key={status}>
                <span>{status === "OFFER" ? "Offer Applications" : `${status[0]}${status.slice(1).toLowerCase()} Applications`}</span>
                <strong>{applicationCounts[status]}</strong>
                <small>{status === "OFFER" ? "Offers received" : `${status.toLowerCase()} status`}</small>
              </div>
            ))}
            <div className="overview-card overview-card-status">
              <span>Resume Status</span>
              <strong>{resumeLoading ? "Loading..." : resume ? "Resume Uploaded" : "No Resume Uploaded"}</strong>
              <small>{resume ? (resumeName || resume.title || "Profile available") : "Upload a resume to personalize matches"}</small>
            </div>
            <div className="overview-card overview-card-score">
              <span>Average Match Score</span>
              <strong>{averageMatchScore === null ? "N/A" : `${averageMatchScore}%`}</strong>
              <small>{jobs.length > 0 ? "Across current recommendations" : "Search for jobs to calculate"}</small>
            </div>
          </div>

          <div className="overview-lower-grid">
            <div className="overview-panel">
              <h3>Application Progress</h3>
              <div className="progress-list">
                {APPLICATION_STATUSES.map((status) => (
                  <div className="progress-item" key={status}>
                    <span>{status === "OFFER" ? "Offers" : status}</span>
                    <strong>{applicationCounts[status]}</strong>
                  </div>
                ))}
              </div>
            </div>

            <div className="overview-panel">
              <h3>Current Search</h3>
              <div className="current-search-details">
                <p><span>Location</span><strong>{location.trim() || "Hyderabad"}</strong></p>
                {role.trim() && <p><span>Role</span><strong>{role.trim()}</strong></p>}
                {skills.trim() && <p><span>Skills</span><strong>{skills.trim()}</strong></p>}
                <p><span>Minimum Match Score</span><strong>{minScore}</strong></p>
              </div>
              {!hasAdditionalFilters && (
                <p className="no-filters-message">No additional filters are active.</p>
              )}
            </div>
          </div>
        </section>
        </div>

        <section className="career-grid" aria-label="Career insights">
          <article className="career-readiness-card">
            <div className="career-card-heading">
              <div>
                <p className="eyebrow">YOUR MOMENTUM</p>
                <h2>Career Readiness</h2>
              </div>
              <div className="readiness-ring" style={{ "--readiness": `${readinessScore}%` }}>
                <strong>{readinessScore}%</strong>
              </div>
            </div>
            <p className="career-card-subtitle">
              {readinessScore >= 75 ? "Good progress" : "A few steps can strengthen your profile"}
            </p>
            <div className="readiness-checklist">
              {readinessChecks.map((item) => (
                <span key={item.label} className={item.complete ? "complete" : ""}>
                  {item.complete ? "✓" : "○"} {item.label}
                </span>
              ))}
            </div>
            <a href="#profile" className="text-action">Complete Profile <span>→</span></a>
          </article>

          <article className="ai-insights-card" id="ai-insights">
            <div className="career-card-heading">
              <div>
                <p className="eyebrow">PERSONALIZED GUIDANCE</p>
                <h2>✦ AI Insights</h2>
              </div>
            </div>
            <div className="insight-list">
              <p><span>◈</span> {averageMatchScore === null ? "Search for jobs to see your match insights." : `Your current recommendations average ${averageMatchScore}% match.`}</p>
              <p><span>✧</span> {resume ? "Your resume is powering personalized job matching." : "Upload a resume to unlock more personalized matches."}</p>
              <p><span>◎</span> {applicationCounts.INTERVIEW > 0 ? "Keep preparing for your upcoming interviews." : "Save promising roles and track every next step."}</p>
            </div>
            <a href="#resume" className="text-action">View Resume Insights <span>→</span></a>
          </article>

          <article className="activity-card">
            <div className="career-card-heading">
              <div>
                <p className="eyebrow">KEEP MOVING</p>
                <h2>Recent Activity</h2>
              </div>
              <a href="#applications" className="text-action">View All</a>
            </div>
            {recentApplications.length > 0 ? (
              <div className="activity-list">
                {recentApplications.map((application) => (
                  <div className="activity-item" key={application.id}>
                    <span className="activity-icon">↗</span>
                    <div>
                      <strong>{getApplicationStatus(application)} · {application.title}</strong>
                      <span>{application.company}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="activity-empty">
                <span className="activity-icon">✦</span>
                <p>Save or apply to a job to see your activity here.</p>
              </div>
            )}
          </article>
        </section>

        <section className="resume-section" id="resume">
          <div className="resume-heading">
            <div>
              <p className="eyebrow">PROFILE INSIGHTS</p>
              <h2>Resume</h2>
              <p className="search-description">
                Upload your resume to personalize your recommendations.
              </p>
            </div>
            <span className="resume-icon" aria-hidden="true">📄</span>
          </div>

          <div className="resume-upload">
            <label className="file-picker">
              <span>{resumeFile ? resumeFile.name : "Choose a resume file"}</span>
              <small>PDF, DOC, or DOCX</small>
              <input
                type="file"
                accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                onChange={(e) => {
                  setResumeFile(e.target.files[0] || null);
                  setResumeMessage("");
                }}
              />
            </label>

            <button
              className="upload-btn"
              onClick={uploadResume}
              disabled={resumeLoading || !resumeFile}
            >
              {resumeLoading ? "Uploading resume..." : "Upload Resume"}
            </button>
          </div>

          {resumeLoading && !resumeFile && (
            <p className="resume-message">Loading resume profile...</p>
          )}

          {resumeMessage && (
            <p className={`resume-message ${resumeMessage.includes("successfully") ? "success" : "error"}`}>
              {resumeMessage}
            </p>
          )}

          {!resume && !resumeLoading && (
            <div className="resume-empty-state">
              <div className="empty-state-icon" aria-hidden="true">↑</div>
              <div>
                <h3>No resume uploaded</h3>
                <p>Upload your resume to get better job recommendations.</p>
                <label className="resume-empty-upload">
                  Upload Resume
                  <input
                    type="file"
                    accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    onChange={(e) => {
                      setResumeFile(e.target.files[0] || null);
                      setResumeMessage("");
                    }}
                  />
                </label>
              </div>
            </div>
          )}

          {resume && (
            <div className="resume-details">
              <div className="resume-detail resume-name-detail">
                <h4>Resume Name</h4>
                <p>{resumeName || resume.title || "Not available in resume"}</p>
                {(resume.uploadedAt || resume.createdAt) && (
                  <small>Uploaded {formatApplicationDate({ createdAt: resume.uploadedAt || resume.createdAt })}</small>
                )}
              </div>

              <div className="resume-detail">
                <h4>Professional Summary</h4>
                <p>{resume.summary || "No professional summary found."}</p>
              </div>

              <div className="resume-detail">
                <h4>Skills</h4>
                {renderSkills(resume.skills).length > 0 ? (
                  <div className="skills">
                    {renderSkills(resume.skills).map((skill, index) => (
                      <span key={index}>{skill}</span>
                    ))}
                  </div>
                ) : (
                  <p>Not available in resume</p>
                )}
              </div>

              <div className="resume-detail">
                <h4>Experience</h4>
                <p>{resume.experience || "Not available in resume"}</p>
              </div>

              <div className="resume-detail">
                <h4>Education</h4>
                <p>{resume.education || "Not available in resume"}</p>
              </div>
            </div>
          )}
        </section>

        <section className="applications-section" id="applications">
          <div className="section-header">
            <div>
              <p className="eyebrow">YOUR JOB SEARCH</p>
              <h2>Applications</h2>
            </div>
            <span>{applications.length} Total</span>
          </div>

          {applicationMessage && (
            <p className="application-message">{applicationMessage}</p>
          )}

          <div className="application-summary" aria-label="Application summary">
            <div className="application-summary-card total">
              <strong>{applications.length}</strong>
              <span>Total Applications</span>
            </div>
            {APPLICATION_STATUSES.map((status) => (
              <div
                className={`application-summary-card status-${status.toLowerCase()}`}
                key={status}
              >
                <strong>{applicationCounts[status]}</strong>
                <span>{status === "OFFER" ? "Offers" : status}</span>
              </div>
            ))}
          </div>

          <div className="application-filters" aria-label="Filter applications by status">
            <span>Filter by status</span>
            <div className="application-filter-buttons">
              {["ALL", ...APPLICATION_STATUSES].map((status) => (
                <button
                  type="button"
                  className={applicationFilter === status ? "active" : ""}
                  key={status}
                  onClick={() => setApplicationFilter(status)}
                >
                  {status === "ALL" ? "All" : status}
                </button>
              ))}
            </div>
          </div>

          {applicationsLoading ? (
            <div className="application-empty-state">
              <p>Loading applications...</p>
            </div>
          ) : applications.length === 0 ? (
            <div className="application-empty-state">
              <h3>No applications yet</h3>
              <p>Save a job to start tracking your progress.</p>
            </div>
          ) : filteredApplications.length === 0 ? (
            <div className="application-empty-state">
              <h3>No applications match this status</h3>
              <p>Try selecting another status filter.</p>
            </div>
          ) : (
            <div className="applications-list">
              {filteredApplications.map((application) => (
                <article className="application-card" key={application.id}>
                  <div className="application-details">
                    <h3>{application.title}</h3>
                    <p className="company">🏢 {application.company}</p>
                    {application.location && (
                      <p className="location">📍 {application.location}</p>
                    )}
                    {formatApplicationDate(application) && (
                      <p className="application-meta">
                      {getApplicationStatus(application) === "SAVED"
                        ? "Saved"
                        : "Applied"}{" "}
                      {formatApplicationDate(application)}
                      </p>
                    )}
                    {application.notes && (
                      <p className="application-notes">
                      <strong>Notes:</strong> {application.notes}
                      </p>
                    )}
                    <span
                      className={`application-status status-${(
                        getApplicationStatus(application)
                      ).toLowerCase()}`}
                    >
                      {getApplicationStatus(application)}
                    </span>
                  </div>

                  <div className="application-actions">
                    <label>
                      <span>Status</span>
                      <select
                        value={getApplicationStatus(application)}
                        disabled={updatingApplicationId === application.id}
                        onChange={(event) =>
                          updateApplicationStatus(
                            application,
                            event.target.value
                          )
                        }
                      >
                        {APPLICATION_STATUSES.map((status) => (
                          <option key={status} value={status}>
                            {status}
                          </option>
                        ))}
                      </select>
                    </label>
                    {updatingApplicationId === application.id && (
                      <span className="application-action-state">Updating...</span>
                    )}
                    <div className="application-links">
                      {application.jobUrl ? (
                        <a
                          href={application.jobUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          View Job
                        </a>
                      ) : (
                        <span>Job details unavailable</span>
                      )}
                      <button
                        type="button"
                        className="delete-application-btn"
                        disabled={deletingApplicationId === application.id}
                        onClick={() => deleteApplication(application.id)}
                      >
                        {deletingApplicationId === application.id
                          ? "Deleting..."
                          : "Delete"}
                      </button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <section className="jobs-section" id="recommended-jobs">

          <div className="section-header">
            <div>
              <p className="eyebrow">YOUR OPPORTUNITIES</p>
              <h2>Top Job Recommendations</h2>
            </div>

            <span>
              {jobs.length} Jobs
            </span>
          </div>

          {loading ? (
            <div className="jobs-grid jobs-loading" aria-label="Loading recommended jobs">
              {[1, 2, 3].map((item) => (
                <div className="job-skeleton" key={item}>
                  <span />
                  <span />
                  <span />
                  <span />
                </div>
              ))}
            </div>
          ) : jobs.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon" aria-hidden="true">⌕</div>
              <h3>No jobs found</h3>
              <p>
                Try adjusting your search filters.
              </p>
            </div>
          ) : (
            <div className="jobs-grid">

              {jobs.map((job, index) => (

                <div
                  className="job-card"
                  key={job.id || index}
                >

                  <div className="job-header">

                    <div>
                      <span className="friendly-badge">Fresher Friendly</span>
                      <h3>
                        {job.title || "Job Opportunity"}
                      </h3>

                      <p className="company">
                        🏢 {job.companyName || "Company not specified"}
                      </p>
                    </div>

                    {job.score !== undefined && (
                      <div className={`score score-${getScoreTier(job.score)}`} aria-label={`Match Score: ${job.score}`}>
                        <strong>{job.score}</strong>
                        <span>Match Score</span>
                      </div>
                    )}

                  </div>

                  {job.location && (
                    <p className="location">
                      📍 {job.location}
                    </p>
                  )}

                  {(job.jobType || job.type || job.experience) && (
                    <div className="job-meta-tags">
                      {(job.jobType || job.type) && (
                        <span>💼 {job.jobType || job.type}</span>
                      )}
                      {job.experience && (
                        <span>🎓 {job.experience}</span>
                      )}
                    </div>
                  )}

                  {job.description && (
                    <p className="description">
                      {job.description}
                    </p>
                  )}

                  <div className="skills-group">
                    <h4>Required skills</h4>
                    {renderSkills(job.requiredSkills).length > 0 ? (
                      <div className="skills">
                        {renderSkills(job.requiredSkills).map((skill, skillIndex) => (
                          <span key={skillIndex}>{skill}</span>
                        ))}
                      </div>
                    ) : (
                      <p className="skills-empty">Not specified</p>
                    )}
                  </div>

                  <div className="match-section">
                    <div className="match-section-heading">
                      <div>
                        <h4>Why this job matches you</h4>
                        <span className="resume-match-label">Based on your resume</span>
                      </div>
                    </div>
                    {renderSkills(job.matchedSkills).length > 0 ? (
                      <div className="matched-skills-list">
                        {renderSkills(job.matchedSkills).map((skill, skillIndex) => (
                          <span className="matched-skill-badge" key={skillIndex}>
                            <span aria-hidden="true">✓</span> {skill}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <p className="no-match-message">No direct resume skill match found</p>
                    )}
                  </div>

                  <div className="job-footer">

                    {job.url ? (
                      <>
                        <a href={job.url} target="_blank" rel="noopener noreferrer">
                          View Job <span aria-hidden="true">→</span>
                        </a>
                        <a className="apply-link" href={job.url} target="_blank" rel="noopener noreferrer">
                          Apply <span aria-hidden="true">↗</span>
                        </a>
                      </>
                    ) : (
                      <span>
                        Job details unavailable
                      </span>
                    )}

                    {findSavedApplication(job) ? (
                      <span className="saved-job-label">
                        Saved as {findSavedApplication(job).status}
                      </span>
                    ) : (
                      <button
                        type="button"
                        className="save-job-btn"
                        disabled={savingJobKey === (job.url || `${job.title}|${job.companyName}`)}
                        onClick={() => saveJob(job)}
                      >
                        {savingJobKey === (job.url || `${job.title}|${job.companyName}`)
                          ? "Saving..."
                          : "Save Job"}
                      </button>
                    )}

                  </div>

                </div>

              ))}

            </div>
          )}

        </section>

      </main>
      </div>
      </div>
    </div>
  );
}

export default App;