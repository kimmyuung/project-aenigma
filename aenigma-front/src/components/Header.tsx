import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Header.css';

export function Header() {
    const { user, isAuthenticated, logout } = useAuth();

    return (
        <header className="header">
            <div className="header-container">
                <Link to="/" className="header-logo">
                    <span className="logo-icon">🎭</span>
                    <span className="logo-text">AENIGMA</span>
                </Link>

                <nav className="header-nav">
                    {isAuthenticated ? (
                        <>
                            <Link to="/rooms" className="nav-link">방 목록</Link>
                            <div className="user-menu">
                                <span className="user-name">
                                    {user?.nickname}
                                    <span className="user-tag">#{user?.displayTag}</span>
                                </span>
                                <button onClick={logout} className="btn btn-ghost">
                                    로그아웃
                                </button>
                            </div>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className="nav-link">로그인</Link>
                            <Link to="/register" className="btn btn-primary">
                                시작하기
                            </Link>
                        </>
                    )}
                </nav>
            </div>
        </header>
    );
}
