import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './AuthPages.css';

export function RegisterPage() {
    const [nickname, setNickname] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { register } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!nickname.trim()) {
            setError('닉네임을 입력해주세요.');
            return;
        }

        if (nickname.length < 2 || nickname.length > 20) {
            setError('닉네임은 2~20자 사이여야 합니다.');
            return;
        }

        setIsLoading(true);
        try {
            await register(nickname);
            navigate('/rooms');
        } catch (err: unknown) {
            const errorMessage = err instanceof Error ? err.message : '회원가입에 실패했습니다.';
            setError(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-container animate-fadeIn">
                <div className="auth-header">
                    <span className="auth-icon">🎭</span>
                    <h1 className="auth-title">AENIGMA</h1>
                    <p className="auth-subtitle">추리 게임의 세계에 오신 것을 환영합니다</p>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="input-group">
                        <label className="input-label">닉네임</label>
                        <input
                            type="text"
                            className="input"
                            placeholder="게임에서 사용할 닉네임"
                            value={nickname}
                            onChange={(e) => setNickname(e.target.value)}
                            maxLength={20}
                            autoFocus
                        />
                    </div>

                    {error && (
                        <div className="error-message">{error}</div>
                    )}

                    <button
                        type="submit"
                        className="btn btn-primary btn-lg btn-block"
                        disabled={isLoading}
                    >
                        {isLoading ? '처리 중...' : '게스트로 시작하기'}
                    </button>
                </form>

                <p className="auth-footer">
                    이미 계정이 있나요?{' '}
                    <Link to="/login">로그인</Link>
                </p>

                <div className="auth-divider">
                    <span>게스트 계정 안내</span>
                </div>

                <p className="auth-info">
                    게스트 계정은 별도의 이메일이나 비밀번호 없이 닉네임만으로 생성됩니다.
                    생성된 <strong>사용자명</strong>을 저장해두면 나중에 다시 로그인할 수 있습니다.
                </p>
            </div>
        </div>
    );
}

export function LoginPage() {
    const [username, setUsername] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!username.trim()) {
            setError('사용자명을 입력해주세요.');
            return;
        }

        setIsLoading(true);
        try {
            await login(username);
            navigate('/rooms');
        } catch (err: unknown) {
            const errorMessage = err instanceof Error ? err.message : '로그인에 실패했습니다.';
            setError(errorMessage);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-container animate-fadeIn">
                <div className="auth-header">
                    <span className="auth-icon">🎭</span>
                    <h1 className="auth-title">다시 오셨군요!</h1>
                    <p className="auth-subtitle">사용자명으로 로그인하세요</p>
                </div>

                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="input-group">
                        <label className="input-label">사용자명</label>
                        <input
                            type="text"
                            className="input"
                            placeholder="GUEST_XXXXXXXX"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            autoFocus
                        />
                    </div>

                    {error && (
                        <div className="error-message">{error}</div>
                    )}

                    <button
                        type="submit"
                        className="btn btn-primary btn-lg btn-block"
                        disabled={isLoading}
                    >
                        {isLoading ? '로그인 중...' : '로그인'}
                    </button>
                </form>

                <p className="auth-footer">
                    계정이 없나요?{' '}
                    <Link to="/register">새로 시작하기</Link>
                </p>
            </div>
        </div>
    );
}
